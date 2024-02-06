import { useState } from 'react';
import { Button, useToast } from "@chakra-ui/react";
import { getData } from "../apis/GetData";

const DownloadFileComponent = () => {
    const [loadingToastId, setLoadingToastId] = useState(null);
    const toast = useToast();

    const handleDownloadFile = async () => {
        try {
            // Show loading toast
            setLoadingToastId(
                toast({
                    title: "Downloading data...",
                    status: "info",
                    duration: 2000, // Indefinite duration
                    isClosable: true,
                })
            );

            // Download data
            const data = await getData();

            // Hide loading toast
            toast.close(loadingToastId);

            // Show success toast
            toast({
                title: "Data downloaded successfully",
                status: "success",
                duration: 3000, // 3 seconds
                isClosable: true,
            });

            console.log(data);
        } catch (error) {
            // Hide loading toast
            if (loadingToastId) {
                toast.close(loadingToastId);
            }

            // Show error toast
            toast({
                title: "Error downloading data",
                description: error.message || "An error occurred while downloading data",
                status: "error",
                duration: 5000, // 5 seconds
                isClosable: true,
            });

            console.error("Error downloading data:", error);
        }
    };

    return (
        <>
            <Button onClick={handleDownloadFile}>Download File</Button>
        </>
    );
};

export default DownloadFileComponent;
