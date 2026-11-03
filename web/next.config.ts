import type { NextConfig } from "next";

// Static export: the site is bundled into the mod jar and served by its built-in HTTP server.
const nextConfig: NextConfig = {
  output: "export",
  images: { unoptimized: true },
};

export default nextConfig;
