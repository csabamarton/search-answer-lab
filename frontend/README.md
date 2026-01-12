# Search Answer Lab - Frontend

React-based user interface for comparing traditional and AI-powered search.

## Technology Stack

- React 18
- TypeScript 5.3
- Vite (build tool & dev server)
- React Router (routing)
- Tailwind CSS (styling)

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── search/              # Search-related components
│   │   │   ├── SearchInput.tsx  # Main search input field
│   │   │   ├── SearchOptions.tsx # Search configuration options
│   │   │   └── ModeToggle.tsx   # Traditional/AI mode toggle
│   │   ├── results/             # Result display components
│   │   │   ├── ResultsList.tsx  # Results container
│   │   │   ├── ResultCard.tsx   # Individual result card
│   │   │   ├── EmptyState.tsx   # No results state
│   │   │   └── LoadingState.tsx # Loading indicator
│   │   ├── metrics/             # Metrics display components
│   │   │   ├── MetricsBar.tsx   # Metrics container
│   │   │   └── MetricCard.tsx   # Individual metric card
│   │   └── layout/              # Layout components
│   │       ├── Header.tsx       # Top navigation
│   │       └── Footer.tsx       # Page footer
│   ├── pages/
│   │   └── SearchPage.tsx       # Main search page
│   ├── services/
│   │   ├── api.ts               # Base API client
│   │   └── searchService.ts     # Search API functions
│   ├── hooks/
│   │   └── useSearch.ts         # Search state management hook
│   ├── types/                   # TypeScript type definitions
│   │   ├── search.ts            # Search-related types
│   │   ├── api.ts               # API types
│   │   └── index.ts             # Type exports
│   ├── utils/
│   │   ├── formatters.ts        # Formatting utilities
│   │   └── constants.ts         # App constants
│   ├── App.tsx                  # Main app component with routing
│   ├── main.tsx                 # App entry point
│   ├── vite-env.d.ts           # Vite environment types
│   └── index.css                # Global styles (Tailwind imports)
├── public/                      # Static assets
├── index.html                   # HTML template
├── vite.config.ts              # Vite configuration
├── tsconfig.json               # TypeScript configuration
├── tsconfig.node.json          # TypeScript config for Node
├── .eslintrc.cjs               # ESLint configuration
├── tailwind.config.js          # Tailwind CSS configuration
├── postcss.config.js           # PostCSS configuration
├── package.json                # Dependencies
└── .env.example                # Environment variables template
```

## Prerequisites

- Node.js 18+
- npm or yarn

## Getting Started

### 1. Install Dependencies

```bash
npm install
```

### 2. Configure Environment

Copy `.env.example` to `.env` and adjust if needed:

```bash
cp .env.example .env
```

Default configuration:
```
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Start Development Server

```bash
npm run dev
```

The app will be available at `http://localhost:3000`

## Available Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Type-check and build production bundle
- `npm run preview` - Preview production build locally
- `npm run lint` - Run ESLint
- `npm run type-check` - Run TypeScript type checking

## Features

### Current Implementation

- **TypeScript**: Full type safety throughout the application
- **Search Input**: Text input with submit button
- **Mode Toggle**: Switch between Traditional and AI search modes
- **Results Display**: Card-based layout for search results
- **Metrics Bar**: Shows search performance metrics
- **Responsive Design**: Mobile-friendly Tailwind CSS styling
- **Loading States**: Loading spinner and empty state placeholders
- **Error Handling**: User-friendly error messages

### To Be Implemented

- Backend integration (API endpoints not yet implemented)
- Search suggestions/autocomplete
- Result pagination
- Advanced filters and sorting
- Search history
- Result highlighting
- Dark mode support

## API Integration

The frontend uses a proxy configuration in `vite.config.ts` to route `/api/*` requests to the backend at `http://localhost:8080`.

### TypeScript Types

All types are defined in `src/types/`:

- `SearchRequest`, `SearchResponse`, `SearchResult`, `SearchMetadata` - Search-related types
- `SearchMode` - Type for search mode ('traditional' | 'ai')
- `ApiError`, `ApiResponse`, `FetchOptions` - API-related types

### Search Service

Located in `src/services/searchService.ts`:

- `executeSearch(query, mode, options)` - Execute search query
- `getSuggestions(query)` - Get search suggestions
- `checkHealth()` - Check API health

### Custom Hook

`useSearch()` hook manages search state:

```tsx
const { results, metadata, loading, error, executeSearch, reset } = useSearch()
```

## Styling

Tailwind CSS is configured for utility-first styling:

- Configuration: `tailwind.config.js`
- Base imports: `src/index.css`
- Custom utilities can be added to Tailwind config

## Development Tips

1. **Hot Reload**: Changes to components automatically reload in browser
2. **API Proxy**: No CORS issues during development thanks to Vite proxy
3. **Component Isolation**: Each component is self-contained and reusable
4. **Environment Variables**: Use `import.meta.env.VITE_*` to access env vars

## Building for Production

```bash
# Build optimized bundle
npm run build

# Preview production build
npm run preview
```

Built files are output to `dist/` directory.

## Browser Support

- Modern browsers (Chrome, Firefox, Safari, Edge)
- ES6+ features required
- No IE11 support

## Next Steps

1. Implement API endpoints in backend
2. Add search result highlighting
3. Implement pagination
4. Add search filters and sorting
5. Implement search history feature
6. Add unit tests with Vitest
7. Add E2E tests with Playwright
