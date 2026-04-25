const express = require('express');
const { chromium } = require('playwright');

const app = express();
app.use(express.text({ limit: '10mb' }));

let browser;

// Launch once
(async () => {
  browser = await chromium.launch({
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  console.log('Chromium launched');
})();

app.post('/generate-pdf', async (req, res) => {
  let page;

  try {
    const html = req.body;

    page = await browser.newPage();

    await page.setContent(html, {
      waitUntil: 'networkidle',
      timeout: 30000
    });

    const pdf = await page.pdf({
      format: 'A4',
      printBackground: true
    });

    res.setHeader('Content-Type', 'application/pdf');
    res.send(pdf);

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
