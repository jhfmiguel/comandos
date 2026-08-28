"use client" // Required directive to support Formik hooks in Next.js App Router

import React, { useState, useEffect, useCallback } from "react"
import { useFormik } from "formik"

// Official imports of compound and primitive components from PrimeReact v11
import { DataTable } from '@primereact/ui/datatable';
import type { DataTablePaginationInstance } from '@primereact/ui/datatable';
import { Paginator } from '@primereact/ui/paginator';
import type { PaginatorPagesInstance, PaginatorRootChangeEvent } from '@primereact/ui/paginator';

// Official icons used in PrimeReact v11 pagination architecture
import { EllipsisH } from '@primeicons/react';
import { AngleDoubleLeft } from '@primeicons/react/angle-double-left';
import { AngleDoubleRight } from '@primeicons/react/angle-double-right';
import { AngleLeft } from '@primeicons/react/angle-left';
import { AngleRight } from '@primeicons/react/angle-right';

import { Layout, Input, InputCPF, Button } from "components"
import { User } from "api/models/users"
import { useUserService } from "api/services/user.service"

// Define the interface for the filter structure
interface QueryUserForm {
    name: string
    cpf: string
}


export const UsersList: React.FC = () => {
    const userService = useUserService();

    // Stores the current page of users fetched from the backend
    const [ users, setUsers ] = useState< User[] >([]);
    
    // Server-side pagination states synced with Spring Boot Page structure
    const [ totalRecords, setTotalRecords ] = useState<number>(0);
    const [ rows, setRows ] = useState<number>(10);
    const [ currentPage, setCurrentPage ] = useState<number>(0);

    // Memoized function to fetch data using your Axios service
    const fetchUsers = useCallback(async (name: string, cpf: string, pageIndex: number, pageSize: number) => {
        try {
            const data = await userService.findUser(name, cpf, pageIndex, pageSize);
            
            // Map your custom Page response metadata to component states
            setUsers(data?.content || []);
            setTotalRecords(data?.totalElements || 0);
        } catch (error) {
            console.error("Failed to fetch users through Axios service:", error);
        }
    }, [userService]);

    // Connected the Formik submission event to trigger fetchUsers with form inputs
    const userSubmit = ( filter: QueryUserForm ) => {
        setCurrentPage(0);
        fetchUsers(filter.name, filter.cpf, 0, rows);
    }

    // Initial load without parameters when component mounts
    useEffect(() => {
        fetchUsers("", "", 0, 10);
    }, []); 

    const {
        handleSubmit: formikSubmit,
        values: filter,
        handleChange: formikChange 
    } = useFormik< QueryUserForm >( {
        onSubmit: userSubmit,
        initialValues: {
            name: '',
            cpf: ''
        }
    } )

        return (
        <Layout title = 'Users'>

            <form onSubmit = { formikSubmit }>
                <div className='columns'>
                    <Input 
                        label = 'Name' 
                        columnClasses = 'is-half'
                        id ='name' 
                        name = 'name'
                        onChange = { formikChange } 
                        value = { filter.name }
                        autoComplete = 'off'
                    />

                    <InputCPF 
                        label = 'CPF' 
                        columnClasses = 'is-half'
                        id ='cpf' 
                        name = 'cpf' 
                        onChange = { formikChange } 
                        value = { filter.cpf }
                        autoComplete = 'off'
                    />
                </div>

                <div className = 'field is-grouped'>
                    <div className = 'control is-link'>
                        <Button label={ 'Find' } type="submit" columnClasses={""} />
                    </div>
                </div>
            </form>

            <div className="columns">
                <div className="column is-full" style={{ width: '100%' }}>

                    <DataTable.Root data={users} paginator defaultRows={10} style={{ width: '100%' }}>
                        
                        <div className="p-3 font-bold" style={{ borderBottom: '1px solid #e5e7eb' }}>
                            Search Results
                        </div>
                        
                        <DataTable.TableContainer style={{ width: '100%', overflowX: 'auto' }}>
                            <DataTable.Table style={{ width: '100%', minWidth: '100%', tableLayout: 'auto' }}>
                                <DataTable.THead>
                                    <DataTable.THeadRow>
                                        <DataTable.THeadCell>ID</DataTable.THeadCell>
                                        <DataTable.THeadCell>Name</DataTable.THeadCell>
                                        <DataTable.THeadCell>CPF</DataTable.THeadCell>
                                        <DataTable.THeadCell>Birth</DataTable.THeadCell>
                                        <DataTable.THeadCell>Address</DataTable.THeadCell>
                                        <DataTable.THeadCell>E-mail</DataTable.THeadCell>
                                        <DataTable.THeadCell>Phone</DataTable.THeadCell>
                                    </DataTable.THeadRow>
                                </DataTable.THead>
                                
                                <DataTable.TBody>
                                    {({ item }: { item: User }) => {
                                        // Helper function to format strings or numbers into a standard Brazilian Date format (dd/MM/yyyy)
                                        const formatDate = (dateString?: string) => {
                                            if (!dateString) return "-";
                                            try {
                                                if (dateString.includes('-')) {
                                                    const [year, month, day] = dateString.split('T')[0].split('-');
                                                    return `${day}/${month}/${year}`;
                                                }
                                                const date = new Date(dateString);
                                                return date.toLocaleDateString('pt-BR');
                                            } catch (e) {
                                                return dateString;
                                            }
                                        };

                                        // FIXED: Added helper function to format raw 11-digit string into standard CPF layout (000.000.000-00)
                                        const formatCPF = (rawCpf?: string) => {
                                            if (!rawCpf) return "-";
                                            
                                            // Removes any unexpected character keeping only digits
                                            const cleanCpf = rawCpf.replace(/\D/g, "");
                                            
                                            // Checks if it has exactly 11 digits to apply the standard regex mask
                                            if (cleanCpf.length === 11) {
                                                return cleanCpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
                                            }
                                            
                                            return rawCpf; // Fallback if the database record is already formatted or partial
                                        };

                                        return (
                                            <DataTable.Row key={item.id}>
                                                <DataTable.Cell>{item.id}</DataTable.Cell>
                                                <DataTable.Cell>{item.name}</DataTable.Cell>
                                                
                                                {/* FIXED: Applied the CPF formatting function on the cell data projection */}
                                                <DataTable.Cell>{formatCPF(item.cpf)}</DataTable.Cell>
                                                
                                                <DataTable.Cell>{formatDate(item.birth)}</DataTable.Cell>
                                                <DataTable.Cell>{item.address}</DataTable.Cell>
                                                <DataTable.Cell>{item.email}</DataTable.Cell>
                                                <DataTable.Cell>{item.phone}</DataTable.Cell>
                                            </DataTable.Row>
                                        );
                                    }}
                                </DataTable.TBody>


                                
                                <DataTable.EmptyTBody>
                                    <DataTable.Row>
                                        <DataTable.Cell colSpan={7}>
                                            <div style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af' }}>
                                                No records found. Try adjusting your search filters.
                                            </div>
                                        </DataTable.Cell>
                                    </DataTable.Row>
                                </DataTable.EmptyTBody>
                            </DataTable.Table>
                        </DataTable.TableContainer>

                        <DataTable.Pagination>
                            {({ page, rows: currentRows, totalRecords: currentTotal, onPageChange }: DataTablePaginationInstance) => {
                                if (currentRows !== rows) setRows(currentRows);
                                if (currentTotal !== totalRecords) setTotalRecords(currentTotal);
                                if (page !== currentPage) setCurrentPage(page);

                                return (
                                    <Paginator.Root
                                        className="py-3 px-3.5 border-t border-surface-200 dark:border-surface-700"
                                        page={page + 1}
                                        total={currentTotal}
                                        itemsPerPage={currentRows}
                                        onPageChange={(e: PaginatorRootChangeEvent) => {
                                            if (e.originalEvent) {
                                                onPageChange(e.originalEvent, e.value - 1);
                                                fetchUsers(filter.name, filter.cpf, e.value - 1, currentRows);
                                            }
                                        }}
                                        style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.5rem' }}
                                    >
                                        <Paginator.Content>
                                            <Paginator.First>
                                                <AngleDoubleLeft />
                                            </Paginator.First>
                                            <Paginator.Prev>
                                                <AngleLeft />
                                            </Paginator.Prev>
                                            <Paginator.Pages>
                                                {({ paginator }: PaginatorPagesInstance) =>
                                                    paginator?.pages.map((p, index) =>
                                                        p.type === 'page' ? (
                                                            <Paginator.Page key={index} value={p.value} />
                                                        ) : (
                                                            <Paginator.Ellipsis key={index}>
                                                                <EllipsisH />
                                                            </Paginator.Ellipsis>
                                                        )
                                                    )
                                                }
                                            </Paginator.Pages>
                                            <Paginator.Next>
                                                <AngleRight />
                                            </Paginator.Next>
                                            <Paginator.Last>
                                                <AngleDoubleRight />
                                            </Paginator.Last>
                                        </Paginator.Content>
                                    </Paginator.Root>
                                );
                            }}
                        </DataTable.Pagination>

                        <div className="p-2 text-right" style={{ borderTop: '1px solid #e5e7eb', fontSize: '0.875rem' }}>
                            Total records: {totalRecords}
                        </div>
                        
                    </DataTable.Root>

                </div>
            </div>

        </Layout>
    )
}

