@echo off
echo Committing and pushing local changes to GitHub...
git add .
git commit -m "Update branding, spacing, deletion flows, and alignment fixes"
git push
echo Done.
