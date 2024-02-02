import { useToast } from '@chakra-ui/react'
import React from 'react'

export default function ToastNotification(title) {
    const toast = useToast()
    const toastIdRef = React.useRef()

    function addToast() {
        toastIdRef.current = toast({ description: title, isClosable: true })
    }
    return (
        <div>
            {addToast}
        </div>
    )
}
