document.getElementById('loginForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorAlert = document.getElementById('errorAlert');
    // 简单前端验证
    if (!username || !password) {
        showError('请输入用户名和密码');
        return;
    }
    try {
        // 显示加载状态
        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="bi bi-arrow-repeat spinner"></i> 登录中...';
        submitBtn.disabled = true;
        // 调用登录API
        const response = await axios.post('/login', {
            username: username,
            password: password
        });
        if (response.data.code === 1) {
            // 登录成功，保存用户信息到本地存储
            localStorage.setItem('currentUser', JSON.stringify(response.data.data));
            localStorage.setItem('isLoggedIn', 'true');
            const token = response.data.data.token; // 提取并保存token
            if (token) {
                localStorage.setItem('token', token);
            }
            console.log("right");
            console.log(response.data.code);
            // 跳转到用户管理页面
            window.location.href = 'user-management.html';
        } else {
            console.log(response.data.code);
            //console.error('登录错误详情:', error);
            showError(response.data.message || '登录失败');
        }
    } catch (error) {
        console.error('登录错误:', error);
        if (error.response && error.response.data) {
            showError(error.response.data.message || '登录失败');
        } else {
            showError('网络错误，请检查连接后重试');
        }
    } finally {
        // 恢复按钮状态
        const submitBtn = document.querySelector('button[type="submit"]');
        submitBtn.innerHTML = '<i class="bi bi-box-arrow-in-right"></i> 登录';
        submitBtn.disabled = false;
    }
});

function showError(message) {
    const errorAlert = document.getElementById('errorAlert');
    const errorMessage = document.getElementById('errorMessage');

    errorMessage.textContent = message;
    errorAlert.classList.remove('d-none');

    // 3秒后自动隐藏错误信息
    setTimeout(() => {
        errorAlert.classList.add('d-none');
    }, 3000);
}

// 检查是否已登录，如果已登录则直接跳转
window.addEventListener('DOMContentLoaded', function () {
    const isLoggedIn = localStorage.getItem('isLoggedIn');
    if (isLoggedIn === 'true') {
        window.location.href = 'user-management.html';
    }
});

// 添加简单的动画效果
document.querySelectorAll('.form-control').forEach(input => {
    input.addEventListener('focus', function () {
        this.parentElement.classList.add('focus');
    });

    input.addEventListener('blur', function () {
        if (!this.value) {
            this.parentElement.classList.remove('focus');
        }
    });
});