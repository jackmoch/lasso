const { test, expect } = require('@playwright/test');
const {
  waitForAppReady,
  getSessionState,
  waitForSessionState,
  clearAppState
} = require('./helpers');

test.describe('Session Lifecycle', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);
  });

  test.describe('Unauthenticated User', () => {
    test('should not show session controls when not logged in', async ({ page }) => {
      const sessionControls = page.getByText(/session controls/i);
      await expect(sessionControls).not.toBeVisible();
    });

    test('should show login button', async ({ page }) => {
      const loginButton = page.getByRole('button', { name: /login with last\.fm/i });
      await expect(loginButton).toBeVisible();
    });
  });

  // Note: For authenticated tests, we would need to either:
  // 1. Mock the OAuth flow
  // 2. Use a test Last.fm account
  // 3. Stub the backend authentication
  //
  // For now, these are placeholders showing what should be tested

  test.describe.skip('Authenticated User - Session Start', () => {
    test.beforeEach(async ({ page, context }) => {
      // TODO: Mock authentication
      // This would set a session cookie or use a test endpoint
      await context.addCookies([{
        name: 'lasso-session',
        value: 'test-session-id',
        domain: 'localhost',
        path: '/',
      }]);

      await page.reload();
      await waitForAppReady(page);
    });

    test('should display session controls when authenticated', async ({ page }) => {
      const sessionControls = page.getByText(/session controls/i);
      await expect(sessionControls).toBeVisible();
    });

    test('should display username input', async ({ page }) => {
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      await expect(usernameInput).toBeVisible();
    });

    test('should enable start button when username entered', async ({ page }) => {
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });

      // Initially disabled
      await expect(startButton).toBeDisabled();

      // Type username
      await usernameInput.fill('targetuser');

      // Should be enabled
      await expect(startButton).toBeEnabled();
    });

    test('should start session when start button clicked', async ({ page }) => {
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });

      await usernameInput.fill('targetuser');
      await startButton.click();

      // Wait for session to start
      await waitForSessionState(page, 'active');

      // Should show pause and stop buttons
      const pauseButton = page.getByRole('button', { name: /pause session/i });
      const stopButton = page.getByRole('button', { name: /stop session/i });

      await expect(pauseButton).toBeVisible();
      await expect(stopButton).toBeVisible();
    });

    test('should show error for invalid username', async ({ page }) => {
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });

      await usernameInput.fill('invaliduser123456789');
      await startButton.click();

      // Should show error message
      const errorMessage = page.getByText(/username.*doesn't exist/i);
      await expect(errorMessage).toBeVisible({ timeout: 10000 });
    });
  });

  test.describe.skip('Authenticated User - Session Controls', () => {
    test.beforeEach(async ({ page, context }) => {
      // TODO: Mock authentication and active session
      await context.addCookies([{
        name: 'lasso-session',
        value: 'test-session-id',
        domain: 'localhost',
        path: '/',
      }]);

      await page.reload();
      await waitForAppReady(page);

      // Start a session
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });
      await usernameInput.fill('targetuser');
      await startButton.click();
      await waitForSessionState(page, 'active');
    });

    test('should pause session', async ({ page }) => {
      const pauseButton = page.getByRole('button', { name: /pause session/i });
      await pauseButton.click();

      await waitForSessionState(page, 'paused');

      // Should show resume button
      const resumeButton = page.getByRole('button', { name: /resume session/i });
      await expect(resumeButton).toBeVisible();

      // Status should show "Paused"
      const pausedStatus = page.getByText(/status:.*paused/i);
      await expect(pausedStatus).toBeVisible();
    });

    test('should resume session', async ({ page }) => {
      // First pause
      const pauseButton = page.getByRole('button', { name: /pause session/i });
      await pauseButton.click();
      await waitForSessionState(page, 'paused');

      // Then resume
      const resumeButton = page.getByRole('button', { name: /resume session/i });
      await resumeButton.click();
      await waitForSessionState(page, 'active');

      // Should show pause button again
      await expect(pauseButton).toBeVisible();

      // Status should show "Active"
      const activeStatus = page.getByText(/status:.*active/i);
      await expect(activeStatus).toBeVisible();
    });

    test('should stop session', async ({ page }) => {
      const stopButton = page.getByRole('button', { name: /stop session/i });
      await stopButton.click();

      // Confirm stop
      const confirmButton = page.getByRole('button', { name: /yes, stop/i });
      await confirmButton.click();

      await waitForSessionState(page, 'not-started');

      // Should show username input again
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      await expect(usernameInput).toBeVisible();

      // Activity feed should be hidden
      const activityFeed = page.getByText(/activity feed/i);
      await expect(activityFeed).not.toBeVisible();
    });

    test('should cancel stop confirmation', async ({ page }) => {
      const stopButton = page.getByRole('button', { name: /stop session/i });
      await stopButton.click();

      // Cancel stop
      const cancelButton = page.getByRole('button', { name: /cancel/i });
      await cancelButton.click();

      // Session should still be active
      const state = await getSessionState(page);
      expect(state).toBe('active');

      // Pause button should still be visible
      const pauseButton = page.getByRole('button', { name: /pause session/i });
      await expect(pauseButton).toBeVisible();
    });
  });

  test.describe.skip('Authenticated User - Activity Feed', () => {
    test.beforeEach(async ({ page, context }) => {
      // TODO: Mock authentication and active session with scrobbles
      await context.addCookies([{
        name: 'lasso-session',
        value: 'test-session-id',
        domain: 'localhost',
        path: '/',
      }]);

      await page.reload();
      await waitForAppReady(page);
    });

    test('should display activity feed when session is active', async ({ page }) => {
      // Start session
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });
      await usernameInput.fill('targetuser');
      await startButton.click();
      await waitForSessionState(page, 'active');

      // Activity feed should be visible
      const activityFeed = page.getByText(/activity feed/i);
      await expect(activityFeed).toBeVisible();

      // Should show session status
      const sessionStatus = page.getByText(/status:/i);
      await expect(sessionStatus).toBeVisible();

      // Should show recent scrobbles section
      const recentScrobbles = page.getByText(/recent scrobbles/i);
      await expect(recentScrobbles).toBeVisible();
    });

    test('should display "no scrobbles" message initially', async ({ page }) => {
      // Start session
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });
      await usernameInput.fill('targetuser');
      await startButton.click();
      await waitForSessionState(page, 'active');

      // Should show empty state
      const emptyMessage = page.getByText(/no scrobbles yet/i);
      await expect(emptyMessage).toBeVisible();
    });

    test('should hide activity feed when session is stopped', async ({ page }) => {
      // Start session
      const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
      const startButton = page.getByRole('button', { name: /start following/i });
      await usernameInput.fill('targetuser');
      await startButton.click();
      await waitForSessionState(page, 'active');

      // Activity feed visible
      let activityFeed = page.getByText(/activity feed/i);
      await expect(activityFeed).toBeVisible();

      // Stop session
      const stopButton = page.getByRole('button', { name: /stop session/i });
      await stopButton.click();
      const confirmButton = page.getByRole('button', { name: /yes, stop/i });
      await confirmButton.click();
      await waitForSessionState(page, 'not-started');

      // Activity feed hidden
      activityFeed = page.getByText(/activity feed/i);
      await expect(activityFeed).not.toBeVisible();
    });
  });
});
