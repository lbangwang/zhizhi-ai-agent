# ===== 前端构建阶段 =====
FROM node:20-alpine AS build
WORKDIR /app

# 先复制依赖清单，利用 Docker 层缓存
COPY package.json package-lock.json ./
RUN npm ci

# 再复制源码并构建（产物输出到 /app/dist）
COPY . .
RUN npm run build

# ===== 运行阶段：nginx 托管静态资源并反向代理 /api =====
FROM nginx:alpine

# 用项目自定义配置覆盖默认站点配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# 前台运行，便于容器编排管理生命周期
CMD ["nginx", "-g", "daemon off;"]
