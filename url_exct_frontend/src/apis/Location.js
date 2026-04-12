const API_KEY = '6324b87095ed001f550a46bf53eac0225460b8e42bfa4e8e135e13ad542e749d'.trim();
const BASE_URL = 'https://api.countrystatecity.in/v1';
const CACHE_TTL = 24 * 60 * 60 * 1000; // 24 hours

const fetchWithCache = async (key, fetchFn) => {
    const cached = localStorage.getItem(key);
    if (cached) {
        const { data, timestamp } = JSON.parse(cached);
        if (Date.now() - timestamp < CACHE_TTL) {
            console.log(`LocationAPI: Loading ${key} from cache`);
            return data;
        }
    }

    const data = await fetchFn();
    if (data && data.length > 0) {
        localStorage.setItem(key, JSON.stringify({ data, timestamp: Date.now() }));
    }
    return data;
};

export const getCountries = async () => {
    return fetchWithCache('countries', async () => {
        try {
            console.log('LocationAPI: Fetching countries from API...');
            const response = await fetch(`${BASE_URL}/countries`, {
                headers: { 'X-CSCAPI-KEY': API_KEY }
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('LocationAPI: Error fetching countries:', error);
            return [];
        }
    });
};

export const getStates = async (countryIso) => {
    if (!countryIso) return [];
    return fetchWithCache(`states_${countryIso}`, async () => {
        try {
            console.log(`LocationAPI: Fetching states for ${countryIso} from API...`);
            const response = await fetch(`${BASE_URL}/countries/${countryIso}/states`, {
                headers: { 'X-CSCAPI-KEY': API_KEY }
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('LocationAPI: Error fetching states:', error);
            return [];
        }
    });
};

export const getCities = async (countryIso, stateIso) => {
    if (!countryIso || !stateIso) return [];
    return fetchWithCache(`cities_${countryIso}_${stateIso}`, async () => {
        try {
            console.log(`LocationAPI: Fetching cities for ${countryIso}-${stateIso} from API...`);
            const response = await fetch(`${BASE_URL}/countries/${countryIso}/states/${stateIso}/cities`, {
                headers: { 'X-CSCAPI-KEY': API_KEY }
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('LocationAPI: Error fetching cities:', error);
            return [];
        }
    });
};
