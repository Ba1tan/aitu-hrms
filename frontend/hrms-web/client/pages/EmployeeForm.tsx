import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { 
  CreateEmployeeRequest,
  employeesApi,
  departmentsApi,
  positionsApi
} from '../../shared/api';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage
} from '@/components/ui/form';
import { useForm } from 'react-hook-form';
import { Input } from '@/components/ui/input';
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue 
} from '@/components/ui/select';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Checkbox } from '@/components/ui/checkbox';
import { CalendarIcon } from 'lucide-react';
import { format } from 'date-fns';
import { cn } from '@/lib/utils';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Card, CardContent } from '@/components/ui/card';

// Схема валидации
const formSchema = z.object({
  firstName: z.string().min(1, 'Обязательное поле'),
  lastName: z.string().min(1, 'Обязательное поле'),
  middleName: z.string().optional(),
  email: z.string().email('Некорректный email'),
  iin: z.string().regex(/^\d{12}$/, 'ИИН должен состоять из 12 цифр').optional().or(z.literal('')),
  phone: z.string().optional(),
  hireDate: z.string().min(1, 'Выберите дату найма'),
  dateOfBirth: z.string().optional(),
  employmentType: z.enum(['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN']),
  baseSalary: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Введите корректную сумму (например, 150000.00)'),
  departmentId: z.string().optional(),
  positionId: z.string().optional(),
  managerId: z.string().optional(),
  bankAccount: z.string().optional(),
  bankName: z.string().optional(),
  resident: z.boolean().default(true),
  hasDisability: z.boolean().default(false),
  pensioner: z.boolean().default(false),
});

type FormData = z.infer<typeof formSchema>;

export default function EmployeeForm() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const employeeId = searchParams.get('id');
  const isEdit = !!employeeId;

  // Справочники
  const { data: departments } = useQuery({
    queryKey: ['departments'],
    queryFn: () => departmentsApi.list().then(res => res.data),
  });

  const { data: positions } = useQuery({
    queryKey: ['positions'],
    queryFn: () => positionsApi.list().then(res => res.data),
  });

  const employeeQuery = useQuery({
    queryKey: ['employee', employeeId],
    queryFn: () => employeesApi.get(employeeId!),
    enabled: !!employeeId,
  });

  const form = useForm<FormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      middleName: '',
      email: '',
      iin: '',
      phone: '',
      employmentType: 'FULL_TIME',
      baseSalary: '',
      resident: true,
      hasDisability: false,
      pensioner: false,
    },
  });

  useEffect(() => {
    const emp = employeeQuery.data?.data;
    if (emp) {
      form.reset({
        firstName: emp.firstName || '',
        lastName: emp.lastName || '',
        middleName: emp.middleName || '',
        email: emp.email || '',
        iin: emp.iin || '',
        phone: emp.phone || '',
        hireDate: emp.hireDate || '',
        dateOfBirth: emp.dateOfBirth || '',
        employmentType: (emp.employmentType as any) || 'FULL_TIME',
        baseSalary: String(emp.baseSalary || ''),
        departmentId: emp.department?.id || '',
        positionId: emp.position?.id || '',
        managerId: emp.manager?.id || '',
        bankAccount: emp.bankAccount || '',
        bankName: emp.bankName || '',
        resident: !!emp.resident,
        hasDisability: !!emp.hasDisability,
        pensioner: !!emp.pensioner,
      });
  }
}, [employeeQuery.data, form]);

  const createMutation = useMutation({
    mutationFn: (data: CreateEmployeeRequest) => employeesApi.create(data),
    onSuccess: () => {
      toast.success('Сотрудник создан');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      navigate('/employees');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Ошибка при создании');
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: Partial<CreateEmployeeRequest>) => employeesApi.update(employeeId!, data),
    onSuccess: () => {
      toast.success('Данные обновлены');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      navigate('/employees');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Ошибка при обновлении');
    }
  });

   const onSubmit = (data: FormData) => {
    const payload: CreateEmployeeRequest = {
      firstName: data.firstName,
      lastName: data.lastName,
      middleName: data.middleName,
      email: data.email,
      iin: data.iin,
      phone: data.phone,
      hireDate: data.hireDate,
      dateOfBirth: data.dateOfBirth,
      employmentType: data.employmentType,
      baseSalary: Number(data.baseSalary), // Конвертируем строку в число для API
      departmentId: data.departmentId,
      positionId: data.positionId,
      managerId: data.managerId,
      bankAccount: data.bankAccount,
      bankName: data.bankName,
      resident: data.resident,
      hasDisability: data.hasDisability,
      pensioner: data.pensioner,
      // РЕШЕНИЕ ОШИБКИ TS2741:
      status: isEdit ? (employeeQuery.data?.data.status || 'ACTIVE') : 'ACTIVE',
    };

    if (isEdit) {
      updateMutation.mutate(payload);
    }   else {
      createMutation.mutate(payload);
    }
  };

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold">
            {isEdit ? 'Редактирование сотрудника' : 'Новый сотрудник'}
          </h1>
          <p className="text-muted-foreground mt-2">
            {isEdit ? `Редактирование записи: ${employeeId}` : 'Заполните основные данные'}
          </p>
        </div>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
          <Card>
            <CardContent className="pt-6">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <FormField
                  control={form.control}
                  name="firstName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Имя *</FormLabel>
                      <FormControl><Input placeholder="Имя" {...field} /></FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Фамилия *</FormLabel>
                      <FormControl><Input placeholder="Фамилия" {...field} /></FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Email *</FormLabel>
                      <FormControl><Input type="email" placeholder="email@example.com" {...field} /></FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <FormField
                  control={form.control}
                  name="hireDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>Дата найма *</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button variant="outline" className={cn("w-full pl-3 text-left font-normal", !field.value && "text-muted-foreground")}>
                              {field.value ? format(new Date(field.value), "dd.MM.yyyy") : <span>Выберите дату</span>}
                              <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value ? new Date(field.value) : undefined}
                            onSelect={(date) => field.onChange(date?.toISOString())}
                            disabled={(date) => date > new Date() || date < new Date("1900-01-01")}
                            initialFocus
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="employmentType"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Тип занятости *</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger><SelectValue placeholder="Выберите тип" /></SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="FULL_TIME">Полный день</SelectItem>
                          <SelectItem value="PART_TIME">Неполный день</SelectItem>
                          <SelectItem value="CONTRACT">Договор</SelectItem>
                          <SelectItem value="INTERN">Стажировка</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="baseSalary"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Оклад (₸) *</FormLabel>
                      <FormControl><Input placeholder="0.00" {...field} /></FormControl>
                      <FormDescription>Числовое значение</FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <FormField
                    control={form.control}
                    name="departmentId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Отдел</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger><SelectValue placeholder="Выберите отдел" /></SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {departments?.map(d => (
                              <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="positionId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Должность</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger><SelectValue placeholder="Выберите должность" /></SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {positions?.map(p => (
                              <SelectItem key={p.id} value={p.id}>{p.title}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </FormItem>
                    )}
                  />
                </div>

                <div className="space-y-3 border-l pl-6">
                  <h3 className="text-sm font-medium">Дополнительно</h3>
                  <FormField
                    control={form.control}
                    name="resident"
                    render={({ field }) => (
                      <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                        <FormControl><Checkbox checked={field.value} onCheckedChange={field.onChange} /></FormControl>
                        <FormLabel>Резидент РК</FormLabel>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="hasDisability"
                    render={({ field }) => (
                      <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                        <FormControl><Checkbox checked={field.value} onCheckedChange={field.onChange} /></FormControl>
                        <FormLabel>Инвалидность</FormLabel>
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="pensioner"
                    render={({ field }) => (
                      <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                        <FormControl><Checkbox checked={field.value} onCheckedChange={field.onChange} /></FormControl>
                        <FormLabel>Пенсионер</FormLabel>
                      </FormItem>
                    )}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={() => navigate('/employees')}>
              Отмена
            </Button>
            <Button 
              type="submit" 
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {isEdit ? 'Сохранить изменения' : 'Создать сотрудника'}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}