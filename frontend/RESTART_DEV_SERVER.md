# Restarting Vite Dev Server

If tooltips aren't showing, try these steps:

1. **Stop the current dev server** (Ctrl+C in the terminal running `npm run dev`)

2. **Clear Vite cache**:
   ```bash
   cd frontend
   rm -rf node_modules/.vite
   # Or on Windows:
   Remove-Item -Recurse -Force node_modules\.vite -ErrorAction SilentlyContinue
   ```

3. **Restart the dev server**:
   ```bash
   npm run dev
   ```

4. **Hard refresh the browser**:
   - Chrome/Edge: Ctrl+Shift+R (Windows) or Cmd+Shift+R (Mac)
   - Firefox: Ctrl+F5 (Windows) or Cmd+Shift+R (Mac)

5. **Check browser console** for any errors or the tooltip debug messages

