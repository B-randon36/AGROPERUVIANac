export const environment = {
  production: true,
  apiBaseUrl: 'http://localhost:48088',
  keycloak: {
    issuer: 'http://localhost:38080/realms/bomerp',
    clientId: 'bomerp-frontend',
    redirectUri: 'http://localhost:4300/auth/callback',
    postLogoutRedirectUri: 'http://localhost:4300/',
  },
};
