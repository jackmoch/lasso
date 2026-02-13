const { test, expect } = require('@playwright/test');
const { waitForAppReady, clearAppState } = require('./helpers');

test.describe('Application Loading', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);
  });

  test('should load the application', async ({ page }) => {
    // Check page title
    await expect(page).toHaveTitle(/Lasso/i);

    // Check that the app container is present
    const appContainer = page.locator('#app');
    await expect(appContainer).toBeVisible();
  });

  test('should display login button when not authenticated', async ({ page }) => {
    // Look for login button
    const loginButton = page.getByRole('button', { name: /login with last\.fm/i });
    await expect(loginButton).toBeVisible();
  });

  test('should display login prompt text', async ({ page }) => {
    // Check for descriptive text
    const promptText = page.getByText(/login with your last\.fm account/i);
    await expect(promptText).toBeVisible();
  });

  test('should not display session controls when not authenticated', async ({ page }) => {
    // Session controls should not be visible
    const sessionControls = page.getByText(/session controls/i);
    await expect(sessionControls).not.toBeVisible();
  });

  test('should not display activity feed when not authenticated', async ({ page }) => {
    // Activity feed should not be visible
    const activityFeed = page.getByText(/activity feed/i);
    await expect(activityFeed).not.toBeVisible();
  });

  test('should have no console errors on initial load', async ({ page }) => {
    const consoleErrors = [];

    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await page.reload();
    await waitForAppReady(page);

    // Filter out known non-critical errors (like missing favicon)
    const criticalErrors = consoleErrors.filter(err =>
      !err.includes('favicon') &&
      !err.includes('Failed to load resource')
    );

    expect(criticalErrors).toHaveLength(0);
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    // App should still load
    await page.reload();
    await waitForAppReady(page);

    // Login button should be visible
    const loginButton = page.getByRole('button', { name: /login with last\.fm/i });
    await expect(loginButton).toBeVisible();
  });
});
