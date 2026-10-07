const menuButton = document.querySelector('.toggle-btn');
const sidebar = document.getElementById('sidebar');

if (menuButton && sidebar) {
    function setMenuOpen(open) {
        sidebar.classList.toggle('active', open);
        menuButton.setAttribute('aria-expanded', String(open));
        menuButton.setAttribute('aria-label', open ? 'Fechar menu' : 'Abrir menu');
    }

    menuButton.addEventListener('click', () => setMenuOpen(!sidebar.classList.contains('active')));
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') setMenuOpen(false);
    });
    document.addEventListener('click', (event) => {
        if (!sidebar.contains(event.target) && !menuButton.contains(event.target)) setMenuOpen(false);
    });
}
