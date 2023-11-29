import { Box, Card, CardBody, Stack } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import ModalComp from '../modals/ModalComp';
import { getImages } from '../../apis/GetImages';

export default function CardComp({ title, body }) {
  const [data, setData] = useState([]);

  useEffect(() => {
    getImages().then((result) => {
      setData(result);
    }).catch((error) => {
      console.error('Error fetching data:', error);
    });
  }, []);

  console.log("IN CO", data);

  return (
    <Stack spacing="true" sx={{ padding: '10px', margin: '10px' }}>
      <Card>
        {/* <CardHeader>
          <Text>{title}</Text>
        </CardHeader> */}
        <CardBody>
          <Box h="100px">
            <p>{body}</p>
          </Box>
        </CardBody>
        <ModalComp title={title} data={data} />
      </Card>
    </Stack>
  );
}
