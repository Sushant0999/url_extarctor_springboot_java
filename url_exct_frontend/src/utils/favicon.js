/**
 * Utility to update the favicon dynamically to show status
 */
export const updateFavicon = (isSearching) => {
    const favicon = document.getElementById('favicon');
    if (!favicon) return;

    const canvas = document.createElement('canvas');
    canvas.width = 64;
    canvas.height = 64;
    const ctx = canvas.getContext('2d');

    // Draw Rounded Background
    const radius = 16;
    ctx.beginPath();
    ctx.moveTo(radius, 0);
    ctx.lineTo(64 - radius, 0);
    ctx.quadraticCurveTo(64, 0, 64, radius);
    ctx.lineTo(64, 64 - radius);
    ctx.quadraticCurveTo(64, 64, 64 - radius, 64);
    ctx.lineTo(radius, 64);
    ctx.quadraticCurveTo(0, 64, 0, 64 - radius);
    ctx.lineTo(0, radius);
    ctx.quadraticCurveTo(0, 0, radius, 0);
    ctx.closePath();
    
    // Deep Slate Gradient (matches UI)
    const gradient = ctx.createLinearGradient(0, 0, 0, 64);
    gradient.addColorStop(0, '#1e293b');
    gradient.addColorStop(1, '#0f172a');
    ctx.fillStyle = gradient;
    ctx.fill();

    // Draw Magnifying Glass (Simplified)
    ctx.strokeStyle = '#6366f1';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(28, 28, 14, 0, Math.PI * 2);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(38, 38);
    ctx.lineTo(50, 50);
    ctx.stroke();

    // Status Indicator
    if (isSearching) {
        ctx.beginPath();
        ctx.arc(50, 14, 8, 0, Math.PI * 2);
        ctx.fillStyle = '#10b981'; // Emerald
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.stroke();
    }

    favicon.href = canvas.toDataURL('image/png');
};
