# HRMS Fix: Router Warnings & Login 400

## Plan Status
- [x] **Step 1**: Create this TODO.md ✅
- [x] **Step 2**: Edit frontend/client/App.tsx - Add future flags to BrowserRouter ✅
- [x] **Step 3**: Edit frontend/client/pages/Login.tsx - Add password validation + test credentials comment ✅
- [x] **Step 4**: Fixed backend DB config mismatch (application-dev.yml) ✅

**🔧 Backend restart needed**:
```
# Kill old java processes
taskkill /F /PID 18232 24628 12976
mvn spring-boot:run -Dspring.profiles.active=dev
```

- [ ] **Step 5**: Test login works, complete


**Test Credentials** (from DB seeds):
| Email | Password | Role |
|-------|----------|------|
| admin@hrms.kz | (BCrypt from V2) try 'password' | SUPER_ADMIN |
| a.zhaksylykova@hrms-demo.kz | password123 | HR_MANAGER |
| z.yessenova@hrms-demo.kz | password123 | ACCOUNTANT |
| n.torekhanov@hrms-demo.kz | password123 | MANAGER |

