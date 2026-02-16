import { Outlet } from 'react-router-dom';
import {
  Header,
  HeaderName,
  Content,
  Theme,
} from '@carbon/react';
import './MainLayout.css';

const MainLayout = () => {
  return (
    <>
      <Theme theme="g100">
        <Header aria-label="Hamza durandhar Hybrid Integration">
          <HeaderName prefix="Hamza">
            durandhar Hybrid Integration
          </HeaderName>
        </Header>
      </Theme>
      <Theme theme="g10">
        <Content
          style={{
            minHeight: 'calc(100vh - 48px)',
            backgroundColor: 'var(--wm-content-bg)',
            padding: '2rem',
          }}
        >
          <Outlet />
        </Content>
      </Theme>
    </>
  );
};

export default MainLayout;

// Made with Bob
