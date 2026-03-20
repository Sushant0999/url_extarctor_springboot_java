import axios from 'axios';

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';

export const getFrontendStatus = async () => {
    try {
        const response = await axios.get(`${baseUrl}/actuator/frontend/status`);
        return response.data;
    } catch (error) {
        console.error('Error fetching frontend status:', error);
        throw error;
    }
};

export const getHealth = async () => {
    try {
        const response = await axios.get(`${baseUrl}/actuator/health`);
        return response.data;
    } catch (error) {
        console.error('Error fetching health:', error);
        throw error;
    }
};

export const getMetrics = async () => {
    try {
        const response = await axios.get(`${baseUrl}/actuator/metrics`);
        return response.data;
    } catch (error) {
        console.error('Error fetching metrics:', error);
        throw error;
    }
};

export const getInfo = async () => {
    try {
        const response = await axios.get(`${baseUrl}/actuator/info`);
        return response.data;
    } catch (error) {
        console.error('Error fetching info:', error);
        throw error;
    }
};
