import { defineConfig } from 'orval'

export default defineConfig({
  api: {
    input: {
      target: '../../../target/openapi/openapi.json',
      validation: false,
      override: {
        transformer: (spec: Record<string, unknown>) => {
          const schemas = (spec as any).components?.schemas
          if (schemas) {
            for (const schema of Object.values(schemas) as any[]) {
              if (schema.properties && !schema.required) {
                schema.required = Object.keys(schema.properties)
              }
            }
          }
          return spec
        },
      },
    },
    output: {
      mode: 'tags-split',
      target: './src/api/generated',
      schemas: './src/api/generated/model',
      client: 'fetch',
      clean: true,
      override: {
        mutator: {
          path: './src/api/mutator/custom-fetch.ts',
          name: 'customFetch',
        },
      },
    },
  },
})
