# /add-frontend

Build frontend component/page: `$ARGUMENTS`

## Step 0 — Read the guide first
```
Read docs/dev-phases/full/frontend.md before writing any code.
Check src/types/index.ts — all interfaces are already defined, use them.
Check src/api/*.api.ts — all API calls already written, import and use them.
```

## Step 1 — Determine what to build
From `$ARGUMENTS`, identify:
- **Page** (route-level) → `src/pages/{module}/{Name}Page.tsx`
- **Dialog** (create/edit form) → `src/pages/{module}/{Name}Dialog.tsx`  
- **Drawer** (detail view) → `src/pages/{module}/{Name}Drawer.tsx`
- **Shared component** → `src/components/common/{Name}.tsx`

## Step 2 — Server data: always React Query, never Zustand

```typescript
// ✅ Data fetching
const { data, isLoading, error } = useQuery({
  queryKey: ['key', ...filterParams],
  queryFn: () => someApi.getAll(params).then(r => r.data.data),
  placeholderData: keepPreviousData,
})

// ✅ Mutations
const mutation = useMutation({
  mutationFn: (data) => someApi.create(data),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['key'] })
    // show success snackbar
  },
  onError: (err) => {
    // show error snackbar
  },
})
```

## Step 3 — Forms: React Hook Form + Yup

```typescript
const schema = yup.object({ field: yup.string().required('Обязательное поле') })
const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
  resolver: yupResolver(schema),
  defaultValues: existingData ?? {},
})
```

## Step 4 — Permissions: always use usePermissions()

```typescript
const { canManageEmployees, canApproveLeave, isEmployee } = usePermissions()
// Gate entire pages with PrivateRoute in App.tsx
// Gate individual buttons/sections inline with {permission && <Component />}
```

## Step 5 — Required UI patterns

**Loading state:** `if (isLoading) return <CircularProgress />`  
**Error state:** `if (error) return <Alert severity="error">Ошибка загрузки данных</Alert>`  
**Empty state:** show `Typography color="text.secondary"` with helpful message  
**Confirmation dialogs:** always use MUI `Dialog` — never `window.confirm()`  
**Snackbar feedback:** use MUI `Snackbar` + `Alert` for success/error mutations  

## Step 6 — Language

All user-facing text in **Russian**. Code in English.  
Use `formatKZT()` for ALL money values.  
Use `formatDate()` for ALL dates.  
Use status label/color maps from `src/utils/format.ts` for all status Chips.

## Step 7 — Wrap in Layout

Every page must be wrapped in `<Layout>`:
```typescript
import Layout from '../../components/common/Layout'

export default function MyPage() {
  return (
    <Layout>
      {/* content */}
    </Layout>
  )
}
```

## Step 8 — Register the route

If adding a new page, add it to `src/App.tsx` inside `<Routes>`:
```typescript
<Route path="/new-page" element={
  <PrivateRoute><NewPage /></PrivateRoute>
} />
```

And add it to the `NAV_ITEMS` array in `src/components/common/Layout.tsx` if it needs sidebar navigation.
