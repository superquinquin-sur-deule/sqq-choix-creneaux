package fr.sqq.mediator;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.logging.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class QuarkusMediator implements Mediator {

    private static final Logger LOG = Logger.getLogger(QuarkusMediator.class);

    private final Map<Class<?>, CommandHandler<?, ?>> commandHandlers = new IdentityHashMap<>();
    private final Map<Class<?>, QueryHandler<?, ?>> queryHandlers = new IdentityHashMap<>();
    private final List<PipelineBehavior> pipelineBehaviors = new ArrayList<>();

    @SuppressWarnings("unchecked")
    void onStartup(@Observes StartupEvent event, BeanManager beanManager) {
        Set<Bean<?>> allBeans = beanManager.getBeans(Object.class, Any.Literal.INSTANCE);

        for (Bean<?> bean : allBeans) {
            Class<?> beanClass = bean.getBeanClass();

            if (CommandHandler.class.isAssignableFrom(beanClass) && beanClass != QuarkusMediator.class) {
                Class<?> commandType = resolveTypeArgFromClass(beanClass, CommandHandler.class);
                if (commandType != null) {
                    CommandHandler<?, ?> handler = (CommandHandler<?, ?>) beanManager.getReference(
                            bean, beanClass, beanManager.createCreationalContext(bean));
                    if (commandHandlers.containsKey(commandType)) {
                        throw new MediatorException("Duplicate CommandHandler for " + commandType.getName());
                    }
                    commandHandlers.put(commandType, handler);
                }
            } else if (PipelineBehavior.class.isAssignableFrom(beanClass)) {
                PipelineBehavior behavior = (PipelineBehavior) beanManager.getReference(
                        bean, beanClass, beanManager.createCreationalContext(bean));
                pipelineBehaviors.add(behavior);
            } else if (QueryHandler.class.isAssignableFrom(beanClass)) {
                Class<?> queryType = resolveTypeArgFromClass(beanClass, QueryHandler.class);
                if (queryType != null) {
                    QueryHandler<?, ?> handler = (QueryHandler<?, ?>) beanManager.getReference(
                            bean, beanClass, beanManager.createCreationalContext(bean));
                    if (queryHandlers.containsKey(queryType)) {
                        throw new MediatorException("Duplicate QueryHandler for " + queryType.getName());
                    }
                    queryHandlers.put(queryType, handler);
                }
            }
        }

        pipelineBehaviors.sort(Comparator.comparingInt(PipelineBehavior::order));

        LOG.infof("Mediator: registered %d command handlers, %d query handlers, %d pipeline behaviors",
                commandHandlers.size(), queryHandlers.size(), pipelineBehaviors.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        CommandHandler<Command<R>, R> handler =
                (CommandHandler<Command<R>, R>) commandHandlers.get(command.getClass());
        if (handler == null) {
            throw new MediatorException("No CommandHandler registered for " + command.getClass().getName());
        }

        PipelineBehavior.Next<R> pipeline = () -> handler.handle(command);
        for (int i = pipelineBehaviors.size() - 1; i >= 0; i--) {
            PipelineBehavior behavior = pipelineBehaviors.get(i);
            PipelineBehavior.Next<R> next = pipeline;
            pipeline = () -> behavior.handle(command, next);
        }
        return pipeline.invoke();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        QueryHandler<Query<R>, R> handler =
                (QueryHandler<Query<R>, R>) queryHandlers.get(query.getClass());
        if (handler == null) {
            throw new MediatorException("No QueryHandler registered for " + query.getClass().getName());
        }
        return handler.handle(query);
    }

    private static Class<?> resolveTypeArgFromClass(Class<?> clazz, Class<?> targetInterface) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Type genericInterface : current.getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType pt) {
                    Type rawType = pt.getRawType();
                    if (rawType == targetInterface) {
                        Type arg = pt.getActualTypeArguments()[0];
                        if (arg instanceof Class<?> c) {
                            return c;
                        }
                        if (arg instanceof ParameterizedType argPt) {
                            return (Class<?>) argPt.getRawType();
                        }
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
