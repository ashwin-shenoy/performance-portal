import axios from 'axios';
import API_BASE_URL from '../config/api';

// Create axios instance with increased timeout for file uploads
// Uses shared API base URL. Existing endpoints with /api/v1 prefix are normalized in request interceptor.
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000, // 5 minutes for large file uploads
  // Don't set default Content-Type - let it be set per request
  // This allows multipart/form-data for file uploads and application/json for API calls
});

const tokenStorage = window.sessionStorage;

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    // Normalize URLs so both styles work:
    // - relative: /tests/123  -> /api/v1/tests/123 (via baseURL)
    // - prefixed: /api/v1/tests/123 -> /tests/123 (to avoid /api/v1/api/v1/...)
    if (typeof config.url === 'string' && config.url.startsWith('/api/v1/')) {
      config.url = config.url.replace('/api/v1', '');
    }

    const token = tokenStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If error is 401 and we haven't retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Try to refresh token
        const refreshToken = tokenStorage.getItem('refreshToken');
        if (refreshToken) {
          const response = await axiosInstance.post('/auth/refresh', {
            refreshToken,
          });

          const { token } = response.data;
          tokenStorage.setItem('token', token);

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return axiosInstance(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, logout user
        tokenStorage.removeItem('token');
        tokenStorage.removeItem('refreshToken');
        tokenStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;

// Made with Bob
