import axios from 'axios';

// Create axios instance with increased timeout for file uploads
// No baseURL - endpoints should include full path (e.g., /api/v1/...)
const axiosInstance = axios.create({
  timeout: 300000, // 5 minutes for large file uploads
  // Don't set default Content-Type - let it be set per request
  // This allows multipart/form-data for file uploads and application/json for API calls
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    console.log('[Axios] Request:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      baseURL: config.baseURL,
      fullURL: `${config.baseURL}${config.url}`,
      headers: config.headers,
      data: config.data
    });
    
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('[Axios] Added auth token to request');
    } else {
      console.log('[Axios] No auth token found');
    }
    return config;
  },
  (error) => {
    console.error('[Axios] Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => {
    console.log('[Axios] Response:', {
      status: response.status,
      statusText: response.statusText,
      url: response.config.url,
      data: response.data
    });
    return response;
  },
  async (error) => {
    console.error('[Axios] Response error:', {
      message: error.message,
      code: error.code,
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data
    });
    
    const originalRequest = error.config;

    // If error is 401 and we haven't retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      console.log('[Axios] 401 error, attempting token refresh');
      originalRequest._retry = true;

      try {
        // Try to refresh token
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          console.log('[Axios] Refreshing token...');
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });

          const { token } = response.data;
          localStorage.setItem('token', token);
          console.log('[Axios] Token refreshed successfully');

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return axiosInstance(originalRequest);
        } else {
          console.log('[Axios] No refresh token available');
        }
      } catch (refreshError) {
        console.error('[Axios] Token refresh failed:', refreshError);
        // Refresh failed, logout user
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;

// Made with Bob
