import { test } from '@playwright/test';

test('should spin up 3 clients and join table', async ({ browser }) => {
  test.setTimeout(120000);

  const context1 = await browser.newContext();
  const context2 = await browser.newContext();
  const context3 = await browser.newContext();

  const page1 = await context1.newPage();
  const page2 = await context2.newPage();
  const page3 = await context3.newPage();

  const playerNames = ['Player1', 'Player2', 'Player3'];
  const pages = [page1, page2, page3];

  for (let i = 0; i < pages.length; i++) {
    const page = pages[i];
    const name = playerNames[i];

    await page.goto('/');

    await page.getByRole('button', { name: 'Join Table' }).click();

    await page.locator('input[placeholder="Your name"]').fill(name);

    await page.getByRole('button', { name: 'Join', exact: true }).click();

    await page.waitForSelector('.bg-\\[\\#35654d\\]', { timeout: 30000 });
  }

  await context1.close();
  await context2.close();
  await context3.close();
});