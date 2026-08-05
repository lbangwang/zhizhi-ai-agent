# 阿里云轻量应用服务器部署清单

面向「给面试官一个公网网址即可体验」的最小可用部署。  
推荐形态：**一台轻量服务器 + Docker Compose（MySQL/Redis）+ 后端 JAR + Nginx（前端 + 反代 `/api`）+ HTTPS**。

> 服务器已经买好、能 SSH 登录时：**直接从下方「首次上线：逐步操作」开始**。  
> 代码有更新时：跳到 **「日常更新：重新发布前后端」**（本机打包 → scp → 服务器重启）。  
> 2 核 2G 机器务必做 **第 2 步 swap + JVM 限内存**，否则容易 OOM。

---

## 首次上线：逐步操作（照着做）

以下默认：Ubuntu、公网 IP 已知、防火墙已放行 **22 / 80**（有域名证书后再开 **443**）。

### 第 1 步：SSH 登录

本机终端：

```bash
ssh root@你的公网IP
# 或：ssh ubuntu@你的公网IP
```

### 第 2 步：加 2G Swap（2G 内存机器强烈建议）

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

### 第 3 步：安装基础软件

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 nginx git curl unzip
sudo systemctl enable --now docker
sudo usermod -aG docker $USER   # 若用非 root，需重新登录生效

# Java 21
sudo apt install -y openjdk-21-jdk
java -version

# Node 20（服务器上构建前端用；也可本机 npm run build 后上传 dist）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v && npm -v
```

### 第 4 步：拉取代码

```bash
sudo mkdir -p /opt && sudo chown -R $USER:$USER /opt
cd /opt
# 换成你的仓库地址（私有库用 SSH key 或 token）
git clone <你的仓库HTTPS或SSH地址> zhizhi-ai-agent
cd zhizhi-ai-agent
```

若不便 git：本机打包上传也可以：

```bash
# 本机执行（在项目根）
tar --exclude=node_modules --exclude=target --exclude=.git \
  -czf zhizhi-src.tgz .
scp zhizhi-src.tgz root@公网IP:/opt/
# 服务器上
mkdir -p /opt/zhizhi-ai-agent && cd /opt/zhizhi-ai-agent
tar -xzf /opt/zhizhi-src.tgz
```

### 第 5 步：配置 `.env`

```bash
cd /opt/zhizhi-ai-agent
cp .env.example .env
nano .env   # 或 vim
```

至少改成（密码请自拟强密码）：

```bash
DASHSCOPE_API_KEY=你的通义Key
# 可选：SEARCH_API_KEY / DEEPSEEK_API_KEY 等

SERVER_PORT=8123
SA_TOKEN_JWT_SECRET=请换成很长的随机串

MYSQL_ENABLED=true
# 重要：URL 必须加引号，否则 source .env 时 & 会被 shell 拆成后台任务，导致变量错乱
MYSQL_URL='jdbc:mysql://127.0.0.1:3306/zhizhi_ai_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的MySQL强密码

REDIS_ENABLED=true
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=你的Redis强密码

KNOWLEDGE_FILE_DIR=/opt/zhizhi-ai-agent/data/knowledge-files
KNOWLEDGE_VECTOR_STORE_FILE=/opt/zhizhi-ai-agent/data/vector-store/knowledge-simple.json
MCP_ENABLED=false
```

### 第 6 步：启动 MySQL + Redis

```bash
cd /opt/zhizhi-ai-agent
# 与 .env 中密码一致（compose 读 MYSQL_PASSWORD / REDIS_PASSWORD）
export MYSQL_PASSWORD='你的MySQL强密码'
export REDIS_PASSWORD='你的Redis强密码'
sudo -E docker compose up -d
sudo docker compose ps
# 等 healthy 后再继续
```

### 第 7 步：构建并启动后端

```bash
cd /opt/zhizhi-ai-agent
mkdir -p data/knowledge-files data/vector-store data/artifacts logs

# 必须用 bash source（zsh 会因 MYSQL_URL 里的 & 报错）
bash -lc 'set -a; source /opt/zhizhi-ai-agent/.env; set +a; cd /opt/zhizhi-ai-agent && ./mvnw -DskipTests package'

# 2G 内存务必限制堆：
bash -lc 'set -a; source /opt/zhizhi-ai-agent/.env; set +a; \
  nohup java -Xms256m -Xmx512m -jar /opt/zhizhi-ai-agent/target/zhizhi-ai-agent-0.0.1-SNAPSHOT.jar \
  > /opt/zhizhi-ai-agent/logs/backend.log 2>&1 &'

sleep 8
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8123/api/health
# 期望 200；否则：tail -n 100 /opt/zhizhi-ai-agent/logs/backend.log
```

### 第 8 步：构建前端并放到 Nginx 目录

```bash
cd /opt/zhizhi-ai-agent/zhizhi-ai-agent-frountend
cp -n .env.example .env
# 确认 VITE_API_BASE= 为空（同域走 /api）
grep VITE_API_BASE .env

npm ci
npm run build

sudo mkdir -p /var/www/zhizhi-web
sudo rm -rf /var/www/zhizhi-web/*
sudo cp -r dist/* /var/www/zhizhi-web/
```

### 第 9 步：配置 Nginx（先 HTTP，域名后再上 HTTPS）

```bash
sudo tee /etc/nginx/sites-available/zhizhi.conf >/dev/null <<'EOF'
server {
    listen 80;
    server_name _;   # 有域名后改成 demo.xxx.com

    root /var/www/zhizhi-web;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8123/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/zhizhi.conf /etc/nginx/sites-enabled/zhizhi.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

本机浏览器打开：`http://你的公网IP`  
能进首页、能登录、Workspace 能流式对话，即部署成功。

### 第 10 步（可选）：域名 + HTTPS

1. 域名 A 记录 → 公网 IP。  
2. 安装证书（示例 certbot）：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d demo.yourdomain.com
```

3. 把 Nginx `server_name` 改成域名后 `sudo nginx -t && sudo systemctl reload nginx`。  
4. 之后把链接 `https://demo.yourdomain.com` 发给面试官。

### 第 11 步：演示前自检

- [ ] 首页可开  
- [ ] 注册/登录  
- [ ] Workspace 流式回复  
- [ ] 停止按钮有效  
- [ ] HITL 写文件可拒绝  
- [ ] 轻量控制台确认 **未放行** 3306 / 6379 / 8123  

---

## 日常更新：重新发布前后端（照着做）

> **推荐流程**：本机构建 → `scp` 上传 → 服务器替换并重启。  
> 部分机房（尤其国内轻量）**访问不了 GitHub / Docker Hub**，不要依赖服务器上 `git pull`。  
> 路径约定：代码 `/opt/zhizhi-ai-agent`，前端站点 `/var/www/zhizhi-web`，公网入口走 Nginx `:80`。

### 只改哪一端就做哪一段

| 改动 | 需要做 |
|------|--------|
| 只改后端 Java | A（本机打后端包）→ B（服务器更新后端） |
| 只改前端 Vue | A（本机打前端包）→ C（服务器更新前端） |
| 两端都改 | A（两个包）→ B → C |
| 只改服务器 `.env` | 不用打包，直接做 B 的「加载环境变量 + 重启」 |
| 只改 Nginx | 改配置后 `sudo nginx -t && sudo systemctl reload nginx` |

---

### A. 本机打包并上传

在 Mac 项目路径执行（按本机实际路径调整）：

```bash
# ---------- 后端（有 Java 改动时）----------
cd /Users/zhizhi/IdeaProjects/zhizhi-ai-agent
./mvnw -DskipTests package

# COPYFILE_DISABLE=1 避免 Mac 打出 ._xxx / LIBARCHIVE 扩展属性干扰 Linux 解压
COPYFILE_DISABLE=1 tar -czf /tmp/zhizhi-backend.tgz \
  target/zhizhi-ai-agent-0.0.1-SNAPSHOT.jar \
  src/main/resources/db

# ---------- 前端（有 Vue 改动时）----------
cd /Users/zhizhi/IdeaProjects/zhizhi-ai-agent/zhizhi-ai-agent-frountend
npm run build
# 注意：打包的是 dist 目录「内容」，解压后直接是 index.html / assets/
COPYFILE_DISABLE=1 tar -czf /tmp/zhizhi-frontend-dist.tgz -C dist .

# ---------- 上传到服务器 ----------
scp /tmp/zhizhi-backend.tgz /tmp/zhizhi-frontend-dist.tgz root@你的公网IP:/tmp/
```

首次 `scp` / `ssh` 若提示主机指纹，输入 **`yes`** 后回车；若报 `Host key verification failed`：

```bash
ssh-keygen -R 你的公网IP
```

---

### B. 服务器：更新并重启后端

```bash
cd /opt/zhizhi-ai-agent

# 1) 解压新 jar
mkdir -p /tmp/zhizhi-backend-unpack
tar -xzf /tmp/zhizhi-backend.tgz -C /tmp/zhizhi-backend-unpack
mkdir -p /opt/zhizhi-ai-agent/target
cp /tmp/zhizhi-backend-unpack/target/zhizhi-ai-agent-0.0.1-SNAPSHOT.jar \
  /opt/zhizhi-ai-agent/target/

# 若有新增表结构，按需执行（仅在有 SQL 变更时）
# mysql -u root -p zhizhi_ai_agent < /tmp/zhizhi-backend-unpack/src/main/resources/db/schema.sql

# 2) 加载环境变量（必须在项目根；MYSQL_URL 必须带引号）
set -a && source /opt/zhizhi-ai-agent/.env && set +a
echo "MYSQL_ENABLED=$MYSQL_ENABLED"
# 必须输出 true。若仍是 false，先改 .env：
#   sed -i 's/^MYSQL_ENABLED=.*/MYSQL_ENABLED=true/' /opt/zhizhi-ai-agent/.env
# 再重新 source。
# 若 source 后出现类似 [4]+ Done serverTimezone=... ，说明 MYSQL_URL 没加引号，请改 .env 后重试。

# 3) 停旧进程，启动新进程（2G 内存务必限堆）
pkill -f 'zhizhi-ai-agent-0.0.1-SNAPSHOT.jar' || true
mkdir -p /opt/zhizhi-ai-agent/logs
nohup java -Xms256m -Xmx512m \
  -jar /opt/zhizhi-ai-agent/target/zhizhi-ai-agent-0.0.1-SNAPSHOT.jar \
  > /opt/zhizhi-ai-agent/logs/backend.log 2>&1 &

# 4) 健康检查
sleep 12
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8123/api/health
# 期望 200；失败则：tail -n 100 /opt/zhizhi-ai-agent/logs/backend.log
```

注册接口自检（可选）：

```bash
curl -s -i -X POST http://127.0.0.1:8123/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user_01","password":"123456","nickname":"demo"}'
# 正常应返回 code:0 与 token；若 status:404，多半是 MYSQL_ENABLED 未生效（Auth 模块未加载）
```

---

### C. 服务器：更新前端并重载 Nginx

```bash
# 1) 解压到临时目录（不要直接解到站点目录）
rm -rf /tmp/zhizhi-web-new
mkdir -p /tmp/zhizhi-web-new
tar -xzf /tmp/zhizhi-frontend-dist.tgz -C /tmp/zhizhi-web-new
find /tmp/zhizhi-web-new -name '._*' -delete
ls -la /tmp/zhizhi-web-new
# 这里应能看到 index.html、assets/、avatars/

# 2) 覆盖站点目录
sudo mkdir -p /var/www/zhizhi-web
sudo rm -rf /var/www/zhizhi-web/*
# 推荐显式拷贝，避免个别环境下「目录看起来拷了其实是空的」
sudo cp -r /tmp/zhizhi-web-new/assets \
           /tmp/zhizhi-web-new/avatars \
           /tmp/zhizhi-web-new/favicon.svg \
           /tmp/zhizhi-web-new/icons.svg \
           /tmp/zhizhi-web-new/index.html \
           /var/www/zhizhi-web/
sudo chown -R www-data:www-data /var/www/zhizhi-web

# 3) 确认文件已就位（空目录会导致 403）
ls -la /var/www/zhizhi-web
ls /var/www/zhizhi-web/assets

# 4) 重载 Nginx
sudo nginx -t && sudo systemctl reload nginx
```

浏览器 **强制刷新**（Ctrl/Cmd+Shift+R）后打开：`http://你的公网IP`。

若本机已构建好完整 `dist`，也可上传「带 dist 目录」的包，解压后用 `/tmp/dist/...` 同样按上面第 2 步拷贝。

---

### D. 常用排查命令

```bash
# 后端进程 / 端口
ss -lntp | grep 8123
tail -n 80 /opt/zhizhi-ai-agent/logs/backend.log

# 环境变量是否生效
set -a && source /opt/zhizhi-ai-agent/.env && set +a
echo "MYSQL_ENABLED=$MYSQL_ENABLED REDIS_ENABLED=$REDIS_ENABLED"

# 直连后端 vs 经 Nginx
curl -s -o /dev/null -w "direct %{http_code}\n" http://127.0.0.1:8123/api/health
curl -s -o /dev/null -w "nginx  %{http_code}\n" http://127.0.0.1/api/health

# 前端静态资源
ls -la /var/www/zhizhi-web
ls -la /etc/nginx/sites-enabled/
```

| 现象 | 常见原因 | 处理 |
|------|----------|------|
| 打开 IP 是 Welcome to nginx | 仍在用默认站点 | 启用 `zhizhi.conf`，删除 `sites-enabled/default`，`nginx -t && reload` |
| `403 Forbidden` | `/var/www/zhizhi-web` 为空或无 `index.html` | 重新执行 C，确认 `ls` 有文件 |
| 注册/登录 `404`，health 却是 `200` | `MYSQL_ENABLED!=true`，Auth 未加载 | 改 `.env` 为 `true`，重新 source 后按 B 重启 |
| `source .env` 出现 `Done serverTimezone` | `MYSQL_URL` 未加引号 | URL 整段用单引号包起来再 source |
| 知识库页白屏 | 前端旧包缺陷或资源未更新 | 本机重新 `npm run build`，按 C 全量覆盖并强刷 |
| Nginx `duplicate default_server` | 多个 server 抢 `default_server` | 去掉多余 `default_server`，只保留一个站点 |
| 聊天无流式 / 一直转圈 | 反代缓冲或后端挂了 | 确认 `proxy_buffering off`；看 `backend.log` |

更稳妥可后续补 `systemd` 服务文件（可选），把「source .env + java -jar」写成开机自启单元。

---

## 0. 采购与规格建议

| 项 | 建议 |
|----|------|
| 产品 | 阿里云 **轻量应用服务器**（SWAS） |
| 地域 | 离你近、延迟低即可（如华东 1） |
| 镜像 | **Ubuntu 22.04**（或带 Docker 的应用镜像） |
| 套餐 | **≥ 2 核 4GB**（Agent + 模型 HTTP 峰值吃内存；1C2G 易 OOM） |
| 系统盘 | ≥ 40GB（产物、向量 JSON、日志） |
| 网络 | 绑定公网 IP；后续可挂自定义域名 |

安全组 / 防火墙仅放行：

| 端口 | 用途 |
|------|------|
| 22 | SSH（建议改端口或仅密钥登录） |
| 80 | HTTP（证书申请 / 跳转 HTTPS） |
| 443 | HTTPS（对外唯一业务入口） |

**不要**对公网开放 `3306`（MySQL）、`6379`（Redis）、`8123`（后端直出）。

---

## 1. 域名与证书

1. 准备域名（阿里云域名或已有域名）A 记录指向轻量公网 IP。  
2. 证书二选一：
   - 阿里云免费 SSL 下载后放到 Nginx；或  
   - 服务器上 `certbot`（Let’s Encrypt）自动签发。  
3. 最终对外地址形如：`https://demo.yourdomain.com`。

---

## 2. 服务器基础环境

SSH 登录后执行（示例）：

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 nginx git curl
sudo systemctl enable --now docker

# Java 21（与本地开发一致）
sudo apt install -y openjdk-21-jdk

# Node 20（仅构建前端时需要；也可本机构建后上传 dist）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

目录建议：

```text
/opt/zhizhi-ai-agent/          # 代码或发布包
/opt/zhizhi-ai-agent/data/     # 产物、向量库、知识库文件（持久化）
/var/www/zhizhi-web/           # 前端 dist
/etc/nginx/sites-available/zhizhi.conf
```

---

## 3. 基础设施：MySQL + Redis

在项目根目录（含 `docker-compose.yml`）：

```bash
cd /opt/zhizhi-ai-agent
# 生产请设置强密码，写入 .env 后：
export MYSQL_PASSWORD='换成强密码'
export REDIS_PASSWORD='换成强密码'
sudo docker compose up -d
sudo docker compose ps
```

初始化：MySQL 首次启动会执行 `src/main/resources/db/schema.sql`。  
若库已存在但缺表，按 README 中各 `db/tables/*.sql` 补齐。

应用连接本机容器时：

```bash
MYSQL_ENABLED=true
MYSQL_URL='jdbc:mysql://127.0.0.1:3306/zhizhi_ai_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
MYSQL_USERNAME=root
MYSQL_PASSWORD=与 compose 一致

REDIS_ENABLED=true
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=与 compose 一致
```

---

## 4. 应用密钥与环境变量

在服务器创建 `/opt/zhizhi-ai-agent/.env`（**勿提交 Git**），参考仓库 `.env.example`，至少配置：

```bash
# 模型（演示至少配一个默认链路，如通义）
DASHSCOPE_API_KEY=...
# 可选
DEEPSEEK_API_KEY=
DOUBAO_API_KEY=
SEARCH_API_KEY=

SERVER_PORT=8123

# 鉴权（必须更换）
SA_TOKEN_JWT_SECRET=请换成足够长的随机字符串

# 数据目录建议放持久盘路径
KNOWLEDGE_FILE_DIR=/opt/zhizhi-ai-agent/data/knowledge-files
KNOWLEDGE_VECTOR_STORE_FILE=/opt/zhizhi-ai-agent/data/vector-store/knowledge-simple.json

# 演示安全默认
MCP_ENABLED=false
HITL_TIMEOUT_SECONDS=120
```

控制台为 Key 设置**额度告警**，避免面试官反复调用把余额打光。

---

## 5. 构建与启动后端

```bash
cd /opt/zhizhi-ai-agent
# 加载环境变量（注意：含 & 的 MYSQL_URL 请用 bash source）
set -a && source .env && set +a

./mvnw -DskipTests package
# 产物一般在 target/*.jar

mkdir -p data/knowledge-files data/vector-store data/artifacts
nohup java -jar target/zhizhi-ai-agent-*.jar > /var/log/zhizhi-backend.log 2>&1 &
# 或使用 systemd 单元（推荐生产）
curl -s http://127.0.0.1:8123/api/health
```

可用 `systemd` 保活示例思路：`WorkingDirectory=/opt/zhizhi-ai-agent`，`EnvironmentFile=/opt/zhizhi-ai-agent/.env`，`ExecStart=/usr/bin/java -jar ...`。

---

## 6. 构建前端

**同域反代时**：构建前保持 `VITE_API_BASE` 为空，浏览器请求走相对路径 `/api`。

```bash
cd /opt/zhizhi-ai-agent/zhizhi-ai-agent-frountend
cp -n .env.example .env
# .env 中 VITE_API_BASE=   （留空）

npm ci
npm run build
sudo rm -rf /var/www/zhizhi-web/*
sudo cp -r dist/* /var/www/zhizhi-web/
```

---

## 7. Nginx：静态站 + `/api` 反代（含 SSE）

`/etc/nginx/sites-available/zhizhi.conf` 示例：

```nginx
server {
    listen 80;
    server_name demo.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name demo.yourdomain.com;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    root /var/www/zhizhi-web;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Spring Boot API + SSE
    location /api/ {
        proxy_pass http://127.0.0.1:8123/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # SSE / 长连接
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
        chunked_transfer_encoding on;
    }
}
```

```bash
sudo ln -sf /etc/nginx/sites-available/zhizhi.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

---

## 8. 上线检查清单（发给面试官前）

- [ ] `https://你的域名` 能打开首页  
- [ ] 注册/登录可用（或已预置账号）  
- [ ] Workspace 对话有 SSE 流式输出（不是卡住无响应）  
- [ ] 点「停止」能结束生成  
- [ ] HITL：写文件弹出确认，拒绝后计划显示已拒绝  
- [ ] 知识库上传与试检索可用  
- [ ] `/trace` 有记录（需 MySQL）  
- [ ] 公网扫端口：3306/6379/8123 **不可达**  
- [ ] 演示账号密码已准备；README 三场景可走通  

预置账号建议：只开 1～2 个，密码当面告知或放在邀请邮件，勿写进公开仓库。

---

## 9. 演示安全建议（轻量机必看）

| 风险 | 建议 |
|------|------|
| 终端命令工具 | 云上演示尽量只展示 HITL「拒绝」，或临时下线该工具 |
| 写文件 / 下载 | 限制工作目录到 `/opt/zhizhi-ai-agent/data` |
| 费用 | 模型与 SearchAPI 设配额；面试结束后可停机 |
| 备份 | 定期备份 MySQL 卷与 `data/` 目录 |

---

## 10. 常见问题

| 现象 | 排查 |
|------|------|
| 页面开得开，聊天无流式 | Nginx 是否 `proxy_buffering off`、超时是否够长 |
| 401 满屏 | JWT Secret、Cookie/跨域；优先同域 `/api` |
| 注册/登录 404，health 200 | `.env` 中 `MYSQL_ENABLED=true` 是否在启动前 `source` 生效 |
| 打开站点 403 / Welcome to nginx | `/var/www/zhizhi-web` 是否有 `index.html`；是否禁用了 default 站点 |
| 停止无效 | `REDIS_ENABLED` 与密码是否与 compose 一致 |
| 上传知识库失败 | `data/` 目录权限、磁盘空间 |
| 机器很卡 | 升配到 4G；避免同机再跑大型本地模型 |
| 更完整的更新步骤与排错表 | 见上文 **「日常更新：重新发布前后端」** |

---

## 11. 成本与关机

- 面试时段开机，结束后可在轻量控制台**关机**省流量与风险（磁盘保留）。  
- 域名解析、证书、MySQL 数据卷在关机后仍在，下次开机按 `docker compose up -d` + 启动 JAR + Nginx 即可。

---

## 相关文档

- 本地启动与功能说明：根目录 [`README.md`](../README.md)  
- 演示口播：[`docs/demo-script.md`](demo-script.md)  
- 架构一页纸：[`docs/architecture.md`](architecture.md)
