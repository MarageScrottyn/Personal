const net = require('net');

const PIPE_NAME = '\\\\.\\pipe\\NativeServicePipe';

const client = new net.Socket();

client.on('connect', () => {
    console.log('Connected to service');

    const request = JSON.stringify({ action: 'get_media' });
    const lengthBuffer = Buffer.alloc(4);
    lengthBuffer.writeUInt32LE(request.length, 0);

    client.write(lengthBuffer);
    client.write(request);
});

client.on('data', (data) => {
    console.log('Received data:', data.toString());
});

client.on('error', (err) => {
    console.error('Socket error:', err);
});

client.on('close', () => {
    console.log('Connection closed');
});

client.connect(PIPE_NAME);