import Axios, { AxiosInstance } from "axios"

export const httpClient: AxiosInstance = Axios.create({
    
    baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
    withCredentials: true
})

httpClient.interceptors.request.use(async config => {
    if (!["get", "head", "options"].includes((config.method ?? "get").toLowerCase())) {
        const { data } = await httpClient.get<{ headerName: string; token: string }>("/api/auth/csrf");
        config.headers.set(data.headerName, data.token);
    }
    return config;
});

httpClient.interceptors.response.use(response => response, error => {
    if (typeof window !== "undefined" && error.response?.status === 401 && !error.config?.url?.includes("/api/auth/")) {
        window.dispatchEvent(new Event("session-expired"));
    }
    if (typeof window !== "undefined" && error.response?.status === 403 && !error.config?.url?.includes("/api/auth/")) {
        window.dispatchEvent(new Event("access-changed"));
    }
    return Promise.reject(error);
});
