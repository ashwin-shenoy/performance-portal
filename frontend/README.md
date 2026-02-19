# Performance Portal - Frontend

React-based frontend application for the Performance Portal.

## Tech Stack

- **React 18** - UI library
- **Vite** - Build tool and dev server
- **Ant Design 5** - UI component library
- **React Router 6** - Routing
- **Axios** - HTTP client
- **Recharts** - Data visualization
- **Day.js** - Date manipulation

## Prerequisites

- Node.js 18+ and npm
- Backend server running on http://localhost:8080

## Installation

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` if needed to change the API base URL.

## Development

Start the development server:

```bash
npm run dev
```

The application will be available at http://localhost:5173

### Hot Reload

The development server supports hot module replacement (HMR). Changes to your code will be reflected immediately without a full page reload.

## Building for Production

Build the application:

```bash
npm run build
```

The built files will be in the `dist` directory.

Preview the production build:

```bash
npm run preview
```

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── components/      # Reusable components
│   │   ├── Layout/      # Layout components
│   │   └── PrivateRoute.jsx
│   ├── contexts/        # React contexts
│   │   └── AuthContext.jsx
│   ├── pages/           # Page components
│   │   ├── Dashboard.jsx
│   │   ├── FileUpload.jsx
│   │   ├── Login.jsx
│   │   ├── Reports.jsx
│   │   └── TestResults.jsx
│   ├── services/        # API services
│   │   └── authService.js
│   ├── utils/           # Utility functions
│   │   └── axios.js
│   ├── config/          # Configuration files
│   │   └── api.js
│   ├── App.jsx          # Main app component
│   ├── main.jsx         # Entry point
│   └── index.css        # Global styles
├── index.html           # HTML template
├── vite.config.js       # Vite configuration
└── package.json         # Dependencies
```

## Features

### Authentication
- JWT-based authentication
- Automatic token refresh
- Protected routes
- Login/logout functionality

### Dashboard
- Performance metrics overview
- Test execution trends
- Response time analytics
- Visual charts and statistics

### File Upload
- Drag-and-drop file upload
- Support for JTL, CSV, and Excel files
- Upload progress tracking
- Capability-based categorization

### Test Results
- View all test runs
- Filter and search functionality
- Detailed test metrics
- Export capabilities

### Reports
- Generate custom reports
- PDF and Excel formats
- Date range filtering
- Download generated reports

## API Integration

The frontend communicates with the backend API through Axios. The base URL is configured in `.env`:

```
VITE_API_BASE_URL=http://localhost:8080
```

### API Endpoints

- **Authentication:** `/auth/login`, `/auth/register`
- **File Upload:** `/upload`
- **Test Runs:** `/tests`, `/tests/{id}`
- **Reports:** `/reports/generate`, `/reports/{id}`
- **Analytics:** `/analytics/summary`, `/analytics/trends`

## Authentication Flow

1. User logs in with credentials
2. Backend returns JWT token
3. Token stored in localStorage
4. Token included in all API requests via Axios interceptor
5. Automatic token refresh on 401 responses
6. Redirect to login on authentication failure

## Default Credentials

For testing purposes:
- **Username:** admin
- **Password:** admin123

## Styling

The application uses Ant Design's theming system. Custom theme configuration is in `App.jsx`:

```javascript
<ConfigProvider
  theme={{
    token: {
      colorPrimary: '#1890ff',
      borderRadius: 6,
    },
  }}
>
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Troubleshooting

### Port Already in Use

If port 5173 is already in use, Vite will automatically try the next available port. You can also specify a different port in `vite.config.js`:

```javascript
server: {
  port: 3000,
}
```

### Backend Connection Issues

1. Ensure the backend server is running on http://localhost:8080
2. Check CORS configuration in the backend
3. Verify the API base URL in `.env`

### Build Errors

Clear node_modules and reinstall:

```bash
rm -rf node_modules package-lock.json
npm install
```

## Development Tips

1. **Use React DevTools** for debugging components
2. **Check Network tab** in browser DevTools for API calls
3. **Use console.log** sparingly; prefer React DevTools
4. **Hot reload** works for most changes; refresh if needed

## Contributing

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Submit a pull request

## License

Copyright © 2024 Hamza. All rights reserved.

## Support

For issues or questions, contact the development team.