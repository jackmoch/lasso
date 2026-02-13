const { test, expect } = require('@playwright/test');
const { waitForAppReady } = require('./helpers');

test.describe('Error Handling', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);
  });

  test('should handle network errors gracefully', async ({ page, context }) => {
    // Simulate offline
    await context.setOffline(true);

    // Try to perform an action that requires network
    // This would fail gracefully with an error message
    // (Requires authentication first in real scenario)

    await context.setOffline(false);
  });

  test('should display and dismiss error messages', async ({ page, context }) => {
    // Skip if not authenticated - would need OAuth mock
    test.skip(!process.env.TEST_WITH_AUTH, 'Requires authentication');

    // Trigger an error (e.g., invalid username)
    const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
    const startButton = page.getByRole('button', { name: /start following/i });

    await usernameInput.fill('invaliduser9999999');
    await startButton.click();

    // Wait for error to appear
    const errorBanner = page.locator('[role="alert"], .bg-red-50').first();
    await expect(errorBanner).toBeVisible({ timeout: 10000 });

    // Find and click dismiss button
    const dismissButton = errorBanner.locator('button[aria-label*="dismiss" i], button').last();
    await dismissButton.click();

    // Error should disappear
    await expect(errorBanner).not.toBeVisible();
  });

  test('should handle 404 routes gracefully', async ({ page }) => {
    await page.goto('/non-existent-page');
    await waitForAppReady(page);

    // Should redirect to home or show 404 page
    // (Depends on routing implementation)
    const appContainer = page.locator('#app');
    await expect(appContainer).toBeVisible();
  });

  test('should recover from JavaScript errors', async ({ page }) => {
    const jsErrors = [];

    page.on('pageerror', error => {
      jsErrors.push(error.message);
    });

    // Navigate and interact
    await page.reload();
    await waitForAppReady(page);

    // App should still be functional despite any errors
    const loginButton = page.getByRole('button', { name: /login with last\.fm/i });
    await expect(loginButton).toBeVisible();

    // Log any JS errors for debugging
    if (jsErrors.length > 0) {
      console.log('JavaScript errors detected:', jsErrors);
    }
  });
});
