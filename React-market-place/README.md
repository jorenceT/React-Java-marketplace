# React + Java E-commerce App

This repository contains a small full-stack e-commerce application:

- `backend/` - Spring Boot REST API for products and orders
- `frontend/` - React storefront built with Vite

## Features

- Product catalog with search and category filters
- Cart management in the browser
- Checkout form that creates an order through the Java backend
- Order history panel for the current demo backend data

## Run locally

### Backend

```powershell
cd backend
.\run.ps1
```

The API runs on `http://localhost:8080`.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173` and proxies `/api` requests to the backend.

If you prefer manual backend steps, this also works:

```powershell
$sources = Get-ChildItem backend/src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d backend/target/classes $sources
java -cp backend/target/classes com.example.ecommerce.EcommerceBackendApplication
```

## Deploy

- GitHub Pages UI workflow: [`.github/workflows/deploy-ui.yml`](D:/coding/React-market-place/.github/workflows/deploy-ui.yml)
- Render blueprint for the API: [`render.yaml`](D:/coding/React-market-place/render.yaml)

Set a repository variable named `VITE_API_BASE_URL` to your Render API URL, for example:

```text
https://your-service.onrender.com/api
```

The GitHub Pages workflow will inject that URL at build time so the static UI can talk to the Render-hosted API.

## API

- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/orders`
- `POST /api/orders`
