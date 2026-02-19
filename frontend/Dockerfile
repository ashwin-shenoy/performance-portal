# Stage 1: Build the React application
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files and install dependencies to leverage Docker layer caching
COPY package*.json ./
RUN npm install

# Copy the rest of the source code and build the application
COPY . .
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html

# Install apache2-utils for htpasswd
RUN apk add --no-cache apache2-utils

# Create htpasswd file with default credentials (should be changed in production)
# Username: admin, Password: change-me-now
RUN htpasswd -cb /etc/nginx/.htpasswd admin change-me-now

# Nginx config to proxy API calls to backend
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
