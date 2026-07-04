#!/bin/bash
echo "=========================================="
echo "  钢铁行业数字化平台 — Demo 启动"
echo "=========================================="
echo ""

# Start AutoRecon backend
echo "[1/5] 启动对账通后端 (端口 8080)..."
cd /workspace/autorecon-server
./mvnw -pl recon-web spring-boot:run -Dspring-boot.run.profiles=demo > /tmp/autorecon-backend.log 2>&1 &
AR_PID=$!
echo "  PID: $AR_PID"

# Start Pickup Express backend
echo "[2/5] 启动提货通后端 (端口 8081)..."
cd /workspace/pickup-express-server
./mvnw -pl pe-web spring-boot:run -Dspring-boot.run.profiles=demo > /tmp/pickup-express-backend.log 2>&1 &
PE_PID=$!
echo "  PID: $PE_PID"

# Wait for backends
echo "[3/5] 等待后端启动..."
sleep 15

# Start AutoRecon frontend
echo "[4/5] 启动对账通前端 (端口 3000)..."
cd /workspace/autorecon-web
npm run dev:demo > /tmp/autorecon-frontend.log 2>&1 &
AF_PID=$!

# Start Pickup Express frontend (port 3001)
echo "[5/5] 启动提货通前端 (端口 3001)..."
cd /workspace/pickup-express-web
PORT=3001 npm run dev:demo > /tmp/pickup-express-frontend.log 2>&1 &
PF_PID=$!

sleep 3
echo ""
echo "=========================================="
echo "  全部启动完成!"
echo "=========================================="
echo ""
echo "  Demo 入口:    file:///workspace/demo-portal/index.html"
echo ""
echo "  对账通前端:   http://localhost:3000"
echo "  对账通API:    http://localhost:8080/doc.html"
echo ""
echo "  提货通前端:   http://localhost:3001"
echo "  提货通API:    http://localhost:8081/doc.html"
echo ""
echo "  Demo账号: admin/admin123, seller1/123456, buyer1/123456"
echo ""
echo "  停止所有: kill $AR_PID $PE_PID $AF_PID $PF_PID"
echo "=========================================="

wait
