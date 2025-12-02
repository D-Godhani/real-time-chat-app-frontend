import type { CodegenConfig } from "@graphql-codegen/cli";
import { config as loadEnv } from "dotenv";

loadEnv();

const schemaUrl =
  process.env.CODEGEN_SCHEMA_URL ??
  (process.env.REACT_APP_API_URL
    ? `${process.env.REACT_APP_API_URL}/graphql`
    : undefined);

if (!schemaUrl) {
  throw new Error(
    "Please set CODEGEN_SCHEMA_URL or REACT_APP_API_URL in your environment."
  );
}

const config: CodegenConfig = {
  overwrite: true,
  schema: schemaUrl,
  documents: "src/**/*.ts",
  generates: {
    "src/gql/": {
      preset: "client",
      plugins: [],
      presetConfig: {
        fragmentMasking: false,
      },
    },
  },
};

export default config;
