/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_MINEDAILY_USERNAME?: string;
  readonly VITE_MINEDAILY_PASSWORD?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
