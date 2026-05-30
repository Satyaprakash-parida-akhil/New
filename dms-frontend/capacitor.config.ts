import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
    appId: 'com.example.dmsfrontend',
    appName: 'DmsFrontend',
    webDir: 'dist/dms-frontend/browser',
    server: {
        androidScheme: 'http',
        cleartext: true,
        allowNavigation: ['192.168.31.152']
    }
};

export default config;
