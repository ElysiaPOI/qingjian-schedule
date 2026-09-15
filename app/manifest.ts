import type { MetadataRoute } from "next"

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "清简课表",
    short_name: "清简课表",
    description: "无广告、可离线使用的个人课表",
    start_url: "/",
    display: "standalone",
    background_color: "#f5f7fb",
    theme_color: "#175cd3",
    orientation: "portrait",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  }
}
