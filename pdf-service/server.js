const express = require('express');
const puppeteer = require('puppeteer-core');

const app = express();
app.use(express.text({ limit: '50mb' }));

const CHROME_PATH = process.env.CHROME_PATH || '/usr/local/bin/chrome-headless-shell';

let browser;

// Launch once
(async () => {
  browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });
  console.log('Chrome launched from:', CHROME_PATH);
})();

app.post('/generate-pdf', async (req, res) => {
  let page;

  try {
    const html = req.body;

    if (!browser || !browser.connected) {
      console.log('Restarting browser...');
      browser = await puppeteer.launch({
        executablePath: CHROME_PATH,
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
      });
    }
    page = await browser.newPage();

    await page.setContent(html, {
      waitUntil: 'domcontentloaded',
      timeout: 30000
    });

    const pdf = await page.pdf({
      format: 'A4',
      printBackground: true
    });

    res.setHeader('Content-Type', 'application/pdf');
    res.send(Buffer.from(pdf));

  } catch (err) {
    console.error('PDF generation failed:', err);
    res.status(500).send('PDF generation failed');
  } finally {
    if (page) await page.close();
  }
});

app.listen(3001, () => {
  console.log('PDF service running on port 3001');
});
