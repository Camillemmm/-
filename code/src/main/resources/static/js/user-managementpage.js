// 全局变量
let currentPage = 1;
const pageSize = 10;
let totalUsers = 0;
let currentUser = null;

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function () {
    checkLoginStatus();
    loadUsers();
    updateStats();
    // 可选：定时更新统计信息（每5分钟更新一次）
    //setInterval(updateStats, 5 * 60 * 1000);
});

// 检查登录状态
function checkLoginStatus() {
    const isLoggedIn = localStorage.getItem('isLoggedIn');
    const currentUserStr = localStorage.getItem('currentUser');
    const token = localStorage.getItem('token');
    /*if (token) {
        // 设置axios默认请求头
        axios.defaults.headers.common['token'] = token;
        console.log('请求头已设置:', axios.defaults.headers.common['token']);
    } else {
        console.error('未找到token，请重新登录');
        window.location.href = 'login.html';
    }*/
    console.log(currentUserStr);
    // 检查登录状态和用户信息
    if (isLoggedIn !== 'true' || !currentUserStr|| !token) {
        redirectToLogin('请先登录');
        return false;
    }

    try {
        currentUser = JSON.parse(currentUserStr);

        // 验证用户信息完整性
        if (!currentUser.id || !currentUser.username) {
            redirectToLogin('用户信息不完整，请重新登录');
            return false;
        }
        // 设置axios默认请求头
        axios.defaults.headers.common['token'] = token;
        // 显示当前用户信息
        displayUserInfo();
        return true;

    } catch (error) {
        console.error('解析用户信息失败:', error);
        redirectToLogin('用户信息解析失败，请重新登录');
        return false;
    }
}

// 显示用户信息
function displayUserInfo() {
    const displayName = currentUser.name || currentUser.username;
    document.getElementById('userGreeting').textContent = `欢迎，${displayName}`;

    const avatarText = (currentUser.name || currentUser.username).charAt(0).toUpperCase();
    document.getElementById('avatarText').textContent = avatarText;
}

// 退出登录函数
function performLogout() {
    // 1. 清除本地存储的登录信息
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('loginTime');
    localStorage.removeItem('token');

    // 2. 关闭模态框
    const logoutModal = bootstrap.Modal.getInstance(document.getElementById('logoutModal'));
    logoutModal.hide();

    // 3. 显示退出成功消息
    showLogoutMessage();

    // 4. 延迟跳转到登录页面
    setTimeout(() => {
        window.location.href = 'login.html';
    }, 1500);
}

// 显示退出成功消息
function showLogoutMessage() {
    // 创建并显示退出成功提示
    const toastContainer = document.createElement('div');
    toastContainer.innerHTML = `
                <div class="position-fixed top-0 start-50 translate-middle-x p-3" style="z-index: 1055;">
                    <div id="logoutToast" class="toast align-items-center text-white bg-success border-0" role="alert">
                        <div class="d-flex">
                            <div class="toast-body">
                                <i class="bi bi-check-circle-fill"></i> 退出登录成功，即将跳转到登录页面...
                            </div>
                            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                        </div>
                    </div>
                </div>
            `;

    document.body.appendChild(toastContainer);

    // 显示Toast提示
    const toast = new bootstrap.Toast(document.getElementById('logoutToast'));
    toast.show();

    // Toast消失后移除元素
    document.getElementById('logoutToast').addEventListener('hidden.bs.toast', function () {
        toastContainer.remove();
    });
}

// 跳转到登录页
function redirectToLogin(reason) {
    console.log(`跳转原因: ${reason}`);
    showError(reason);

    // 清除本地存储的登录信息
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('loginTime');
    localStorage.removeItem('token');

    // 跳转到登录页
    window.location.href = 'login.html';
}

/**
 * 加载用户数据
 * @param {number} page 页码
 * @param {string} keyword 搜索关键字
 * @param {boolean} all 是否查询全部用户
 */
async function loadUsers(page = 1, keyword = '') {
    showLoading(true);
    try {
        currentPage = page;

        const response = await axios.get('/users', {
            params: {
                page: page,
                pagesize: pageSize,
                keyword: keyword
            }
        });

        const users = response.data.data || [];
        
        // 获取总用户数 - 只在第一页或总数未知时调用统计接口
        if (page === 1 || totalUsers === 0) {
            try {
                const statsResponse = await axios.get('/users/stats');
                totalUsers = statsResponse.data.data.totalUsers || 0;
            } catch (error) {
                console.error('获取用户总数失败:', error);
                // 如果统计接口失败，使用当前页数据估算
                totalUsers = users.length;
            }
        } else {
            // 非第一页时，如果当前页数据不满，说明已经到了最后一页
            if (users.length < pageSize) {
                totalUsers = (page - 1) * pageSize + users.length;
            }
        }

        updateStats();
        renderUserTable(users);
        renderPagination();

    } catch (error) {
        console.error('加载用户数据失败:', error);
        //showError('加载用户数据失败，请重试');
    } finally {
        showLoading(false);
    }
}


// 渲染用户表格
function renderUserTable(users) {
    const tbody = document.getElementById('userTableBody');
    tbody.innerHTML = '';

    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">暂无用户数据</td></tr>';
        return;
    }

    users.forEach(user => {
        const row = document.createElement('tr');
        row.innerHTML = `
                    <td><input type="checkbox" class="user-checkbox" value="${user.id}"></td>
                    <td>${user.id}</td>
                    <td>${user.username}</td>
                    <td>${user.name}</td>
                    <td>${user.age}</td>
                    <td>${formatDate(user.updateTime)}</td>
                    <td class="action-buttons">
                        <button class="btn btn-sm btn-outline-primary" onclick="editUser(${user.id})">
                            <i class="bi bi-pencil"></i> 编辑
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteUser(${user.id})">
                            <i class="bi bi-trash"></i> 删除
                        </button>
                    </td>
                `;
        tbody.appendChild(row);
    });
    //给所有新生成的复选框绑定事件
    document.querySelectorAll('.user-checkbox').forEach(cb => {
        cb.addEventListener('change', updateCheckboxState);
    });

    // 更新复选框状态
    updateCheckboxState();
}

// 其他辅助函数
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN');
}

function showLoading(show) {
    document.getElementById('loadingSpinner').style.display = show ? 'block' : 'none';
}

function showError(message) {
    alert(message); // 实际项目中可以使用更优雅的提示方式
}

// 更新统计信息函数
async function updateStats() {
    try {
        const response = await axios.get('/users/stats');
        const stats = response.data.data;

        // 更新页面显示
        document.getElementById('totalUsers').textContent = stats.totalUsers || 0;
        document.getElementById('todayAdded').textContent = stats.todayAdded || 0;
        document.getElementById('activeUsers').textContent = stats.activeUsers || Math.floor(totalUsers * 0.8);
        document.getElementById('avgAge').textContent = stats.avgAge || 0;

    } catch (error) {
        console.error('获取统计信息失败:', error);
        // 失败时显示默认值或错误信息
        document.getElementById('totalUsers').textContent = '--';
        document.getElementById('todayAdded').textContent = '--';
        document.getElementById('activeUsers').textContent = '--';
        document.getElementById('avgAge').textContent = '--';
    }
}


function renderPagination() {
    // 简化的分页实现
    const totalPages = Math.ceil(totalUsers / pageSize);
    const pagination = document.getElementById('pagination');

    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let paginationHTML = '';

    // 上一页
    if (currentPage > 1) {
        paginationHTML += `<li class="page-item"><a class="page-link" href="#" onclick="loadUsers(${currentPage - 1})">上一页</a></li>`;
    }

    // 页码
    for (let i = 1; i <= totalPages; i++) {
        paginationHTML += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="loadUsers(${i})">${i}</a>
                </li>`;
    }

    // 下一页
    if (currentPage < totalPages) {
        paginationHTML += `<li class="page-item"><a class="page-link" href="#" onclick="loadUsers(${currentPage + 1})">下一页</a></li>`;
    }

    pagination.innerHTML = paginationHTML;
}

function toggleSelectAll(checkbox) {
    const checkboxes = document.querySelectorAll('.user-checkbox');
    checkboxes.forEach(cb => {
        cb.checked = checkbox.checked;
    });
    updateCheckboxState();
}

function updateCheckboxState() {
    const checkboxes = document.querySelectorAll('.user-checkbox');
    const checkedCount = Array.from(checkboxes).filter(cb => cb.checked).length;
    document.getElementById('batchDeleteBtn').disabled = checkedCount === 0;
}

// 用户个人信息模态框
function showUserProfile() {
    if (!currentUser) {
        alert('用户信息未加载');
        return;
    }

    // 创建个人信息模态框
    const modalHTML = `
        <div class="modal fade" id="profileModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-person-badge"></i> 个人信息</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-4 text-center">
                                <div class="profile-avatar mb-3">
                                    <div class="user-avatar-large">
                                        <span>${(currentUser.name || currentUser.username).charAt(0).toUpperCase()}</span>
                                    </div>
                                </div>
                                <h5>${currentUser.name || currentUser.username}</h5>
                                <p class="text-muted">${currentUser.username}</p>
                            </div>
                            <div class="col-md-8">
                                <div class="row mb-3">
                                    <div class="col-6">
                                        <label class="form-label"><strong>用户ID</strong></label>
                                        <p>${currentUser.id}</p>
                                    </div>
                                    <div class="col-6">
                                        <label class="form-label"><strong>年龄</strong></label>
                                        <p>${currentUser.age || '未设置'}</p>
                                    </div>
                                </div>
                                <div class="row mb-3">
                                    <div class="col-12">
                                        <label class="form-label"><strong>用户名</strong></label>
                                        <p>${currentUser.username}</p>
                                    </div>
                                </div>
                                <div class="row mb-3">
                                    <div class="col-12">
                                        <label class="form-label"><strong>姓名</strong></label>
                                        <p>${currentUser.name || '未设置'}</p>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-12">
                                        <label class="form-label"><strong>最后更新时间</strong></label>
                                        <p>${formatDate(currentUser.updateTime) || '未知'}</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                        <button type="button" class="btn btn-primary" onclick="changePassword()">修改密码</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // 添加模态框到页面
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    // 显示模态框
    const profileModal = new bootstrap.Modal(document.getElementById('profileModal'));
    profileModal.show();

    // 模态框关闭后移除
    document.getElementById('profileModal').addEventListener('hidden.bs.modal', function () {
        this.remove();
    });
}

// 修改密码功能
function changePassword() {
    const modalHTML = `
        <div class="modal fade" id="changePasswordModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-key"></i> 修改密码</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="passwordForm">
                            <div class="mb-3">
                                <label for="currentPassword" class="form-label">当前密码</label>
                                <input type="password" class="form-control" id="currentPassword" required>
                            </div>
                            <div class="mb-3">
                                <label for="newPassword" class="form-label">新密码</label>
                                <input type="password" class="form-control" id="newPassword" required minlength="6">
                                <div class="form-text">密码长度至少6位</div>
                            </div>
                            <div class="mb-3">
                                <label for="confirmPassword" class="form-label">确认新密码</label>
                                <input type="password" class="form-control" id="confirmPassword" required>
                                <div class="invalid-feedback" id="passwordMatchError">两次输入的密码不一致</div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" onclick="submitPasswordChange()">确认修改</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const passwordModal = new bootstrap.Modal(document.getElementById('changePasswordModal'));
    passwordModal.show();

    // 密码确认验证
    document.getElementById('confirmPassword').addEventListener('input', function () {
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = this.value;
        const errorElement = document.getElementById('passwordMatchError');

        if (newPassword !== confirmPassword) {
            this.classList.add('is-invalid');
            errorElement.style.display = 'block';
        } else {
            this.classList.remove('is-invalid');
            errorElement.style.display = 'none';
        }
    });

    document.getElementById('changePasswordModal').addEventListener('hidden.bs.modal', function () {
        this.remove();
    });
}

// 提交密码修改
async function submitPasswordChange() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!currentPassword || !newPassword || !confirmPassword) {
        alert('请填写所有密码字段');
        return;
    }

    if (newPassword !== confirmPassword) {
        alert('两次输入的新密码不一致');
        return;
    }

    if (newPassword.length < 6) {
        alert('新密码长度至少6位');
        return;
    }

    try {
        // 调用后端API修改密码
        const response = await axios.put('/users/change-password', {
            userId: currentUser.id,
            currentPassword: currentPassword,
            newPassword: newPassword
        });

        const res = response.data;
        //console.log(res);
        if (res.code === 1) {
            alert('密码修改成功');
            const modal = bootstrap.Modal.getInstance(document.getElementById('changePasswordModal'));
            modal.hide();
        } else {
            alert('修改失败: ' + res.msg);
        }

    } catch (error) {
        console.error('修改密码失败:', error);
        alert('修改密码失败: ' + (error.response?.data?.message || error.message));
    }
}

// 添加用户模态框
function showAddUserModal() {
    const modalHTML = `
        <div class="modal fade" id="addUserModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-person-plus"></i> 添加新用户</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="addUserForm">
                            <div class="mb-3">
                                <label for="addUsername" class="form-label">用户名 *</label>
                                <input type="text" class="form-control" id="addUsername" required>
                            </div>
                            <div class="mb-3">
                                <label for="addPassword" class="form-label">密码 *</label>
                                <input type="password" class="form-control" id="addPassword" required minlength="6">
                            </div>
                            <div class="mb-3">
                                <label for="addName" class="form-label">姓名 *</label>
                                <input type="text" class="form-control" id="addName" required>
                            </div>
                            <div class="mb-3">
                                <label for="addAge" class="form-label">年龄</label>
                                <input type="number" class="form-control" id="addAge" min="1" max="150">
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" onclick="submitAddUser()">添加用户</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const addUserModal = new bootstrap.Modal(document.getElementById('addUserModal'));
    addUserModal.show();

    document.getElementById('addUserModal').addEventListener('hidden.bs.modal', function () {
        this.remove();
    });
}

// 提交添加用户
async function submitAddUser() {
    const username = document.getElementById('addUsername').value;
    const password = document.getElementById('addPassword').value;
    const name = document.getElementById('addName').value;
    const age = document.getElementById('addAge').value;

    if (!username || !password || !name) {
        alert('请填写必填字段（用户名、密码、姓名）');
        return;
    }

    if (password.length < 6) {
        alert('密码长度至少6位');
        return;
    }

    try {
        const userData = {
            username: username,
            password: password,
            name: name,
            age: age ? parseInt(age) : null
        };

        const response = await axios.post('users', userData);

        if(!response.data.code){
            alert(response.data.msg);
        }else{
            alert('用户添加成功');
            const modal = bootstrap.Modal.getInstance(document.getElementById('addUserModal'));
            modal.hide();
            // 刷新用户列表
            loadUsers(currentPage);
        }

    } catch (error) {
        console.error('添加用户失败:', error);
        alert('添加用户失败: ' + (error.response?.data?.message || error.message));
    }
}

// 批量删除确认框
function showBatchDeleteModal() {
    const selectedUsers = getSelectedUserIds();

    if (selectedUsers.length === 0) {
        alert('请先选择要删除的用户');
        return;
    }

    const modalHTML = `
        <div class="modal fade" id="batchDeleteModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header bg-danger text-white">
                        <h5 class="modal-title"><i class="bi bi-exclamation-triangle"></i> 确认批量删除</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p>您确定要删除选中的 <strong>${selectedUsers.length}</strong> 个用户吗？</p>
                        <p class="text-danger">此操作不可撤销，请谨慎操作！</p>
                        <div class="selected-users mt-3">
                            <strong>选中的用户ID:</strong>
                            <div class="mt-2">${selectedUsers.join(', ')}</div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-danger" onclick="performBatchDelete()">确认删除</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const batchDeleteModal = new bootstrap.Modal(document.getElementById('batchDeleteModal'));
    batchDeleteModal.show();

    document.getElementById('batchDeleteModal').addEventListener('hidden.bs.modal', function () {
        this.remove();
    });
}

// 获取选中的用户ID
function getSelectedUserIds() {
    const checkboxes = document.querySelectorAll('.user-checkbox:checked');
    return Array.from(checkboxes).map(cb => cb.value);
}

// 执行批量删除
async function performBatchDelete() {
    const selectedUsers = getSelectedUserIds();

    /*if (!selectedUsers || selectedUsers.length === 0) {
        alert("请先选择要删除的用户！");
        return;
    }*/

    // 拼接成 ids=2&ids=3&ids=4
    const qs = selectedUsers.map(id => `ids=${id}`).join('&');

    try {
        const response = await axios.delete(`/users?${qs}`);
        console.log(response.data);
        alert(`成功删除 ${selectedUsers.length} 个用户`);
        const modal = bootstrap.Modal.getInstance(document.getElementById('batchDeleteModal'));
        if (modal) modal.hide();
        loadUsers(currentPage);
    } catch (error) {
        console.error('批量删除失败:', error);
        alert('批量删除失败: ' + (error.response?.data?.message || error.message));
    }
}

async function deleteUser(id) {
    if (!confirm('确定要删除这个用户吗？')) {
        return;
    }

    try {
        const response = await axios.delete(`/users?ids=${id}`);
        if (response.status === 200) {
            loadUsers(currentPage);
            alert('删除成功');
        }
    } catch (error) {
        console.error('删除用户失败:', error);
        alert('删除用户失败，请重试');
    }
}
// 导出用户数据
async function exportUsers() {
    try {
        // 显示导出选项模态框
        const modalHTML = `
            <div class="modal fade" id="exportModal" tabindex="-1">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"><i class="bi bi-download"></i> 导出用户数据</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label">导出格式</label>
                                <div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="exportFormat" id="formatExcel" value="excel" checked>
                                        <label class="form-check-label" for="formatExcel">Excel 文件 (.xlsx)</label>
                                    </div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="exportFormat" id="formatCSV" value="csv">
                                        <label class="form-check-label" for="formatCSV">CSV 文件 (.csv)</label>
                                    </div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="exportFormat" id="formatJSON" value="json">
                                        <label class="form-check-label" for="formatJSON">JSON 文件 (.json)</label>
                                    </div>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">数据范围</label>
                                <div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="exportScope" id="scopeAll" value="all" checked>
                                        <label class="form-check-label" for="scopeAll">全部用户数据</label>
                                    </div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="exportScope" id="scopeCurrent" value="current">
                                        <label class="form-check-label" for="scopeCurrent">当前页数据</label>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="button" class="btn btn-primary" onclick="performExport()">开始导出</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);

        const exportModal = new bootstrap.Modal(document.getElementById('exportModal'));
        exportModal.show();

        document.getElementById('exportModal').addEventListener('hidden.bs.modal', function () {
            this.remove();
        });

    } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败: ' + error.message);
    }
}

// 执行导出
async function performExport() {
    const format = document.querySelector('input[name="exportFormat"]:checked').value;
    const scope = document.querySelector('input[name="exportScope"]:checked').value;

    try {
        // 调用后端导出API
        const response = await axios.get('/users/export', {
            params: {format, scope},
            responseType: 'blob'
        });

        // 创建下载链接
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;

        const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
        link.setAttribute('download', `users_${timestamp}.${format}`);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);

        const modal = bootstrap.Modal.getInstance(document.getElementById('exportModal'));
        modal.hide();

        alert('导出成功！文件已开始下载');

    } catch (error) {
        console.error('导出失败:', error);
        alert('导出失败: ' + error.message);
    }
}

// 导入用户模态框
function showImportModal() {
    const modalHTML = `
        <div class="modal fade" id="importModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-upload"></i> 导入用户数据</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="importFile" class="form-label">选择文件</label>
                            <input class="form-control" type="file" id="importFile" accept=".xlsx,.xls,.csv,.json">
                            <div class="form-text">支持 Excel (.xlsx, .xls), CSV (.csv), JSON (.json) 格式</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">导入选项</label>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" id="skipDuplicates">
                                <label class="form-check-label" for="skipDuplicates">跳过重复用户（根据用户名）</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" id="updateExisting" checked>
                                <label class="form-check-label" for="updateExisting">更新已存在的用户</label>
                            </div>
                        </div>
                        <div id="importPreview" class="mt-3" style="display: none;">
                            <h6>数据预览</h6>
                            <div class="table-responsive">
                                <table class="table table-sm table-bordered">
                                    <thead id="previewHeader"></thead>
                                    <tbody id="previewBody"></tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" id="importButton" disabled onclick="performImport()">开始导入</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const importModal = new bootstrap.Modal(document.getElementById('importModal'));
    importModal.show();

    // 文件选择事件
    document.getElementById('importFile').addEventListener('change', function (e) {
        const file = e.target.files[0];
        if (file) {
            previewImportFile(file);
            document.getElementById('importButton').disabled = false;
        } else {
            // 清空预览
            document.getElementById('importPreview').style.display = 'none';
            document.getElementById('importButton').disabled = true;
        }
    });

    document.getElementById('importModal').addEventListener('hidden.bs.modal', function () {
        this.remove();
    });
}

// 预览导入文件
async function previewImportFile(file) {
    if (!file) {
        alert('请选择要预览的文件');
        return;
    }

    try {
        // 显示加载状态
        document.getElementById('previewBody').innerHTML = `
            <tr>
                <td colspan="3" class="text-center">
                    <div class="spinner-border spinner-border-sm" role="status">
                        <span class="visually-hidden">加载中...</span>
                    </div>
                    正在解析文件...
                </td>
            </tr>
        `;
        document.getElementById('importPreview').style.display = 'block';

        // 调用后端预览接口
        const formData = new FormData();
        formData.append('file', file);

        const response = await axios.post('/users/import/preview', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });

        if (response.data.code === 1) {
            const previewData = response.data.data;
            displayPreviewData(previewData);
        } else {
            throw new Error(response.data.msg || '预览失败');
        }

    } catch (error) {
        console.error('预览失败:', error);
        document.getElementById('previewBody').innerHTML = `
            <tr>
                <td colspan="3" class="text-center text-danger">
                    <i class="bi bi-exclamation-triangle"></i>
                    预览失败: ${error.response?.data?.msg || error.message}
                </td>
            </tr>
        `;
    }
}

// 显示预览数据
function displayPreviewData(data) {
    const { total, previewCount, users, filename } = data;
    
    // 设置表头
    const previewHTML = `
        <tr>
            <th>用户名</th>
            <th>姓名</th>
            <th>年龄</th>
            <th>状态</th>
        </tr>
    `;
    document.getElementById('previewHeader').innerHTML = previewHTML;

    // 生成预览内容
    let previewBodyHTML = '';
    
    if (users && users.length > 0) {
        users.forEach(user => {
            const status = checkUserStatus(user);
            previewBodyHTML += `
                <tr>
                    <td>${user.username || ''}</td>
                    <td>${user.name || ''}</td>
                    <td>${user.age || ''}</td>
                    <td>
                        <span class="badge ${status.class}">${status.text}</span>
                    </td>
                </tr>
            `;
        });
        
        // 如果有更多数据，显示提示
        if (total > previewCount) {
            previewBodyHTML += `
                <tr>
                    <td colspan="4" class="text-center text-muted">
                        <i class="bi bi-info-circle"></i>
                        显示前 ${previewCount} 条数据，共 ${total} 条
                    </td>
                </tr>
            `;
        }
    } else {
        previewBodyHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted">
                    <i class="bi bi-info-circle"></i>
                    未找到有效数据
                </td>
            </tr>
        `;
    }

    document.getElementById('previewBody').innerHTML = previewBodyHTML;
}

// 检查用户状态（是否已存在）
function checkUserStatus(user) {
    // 这里简化处理，实际项目中可以调用后端API检查用户是否存在
    // 暂时返回新用户状态
    return {
        class: 'bg-success',
        text: '新用户'
    };
}

// 显示导入成功提示
function showImportSuccessToast(added, updated, skipped, total) {
    const toastContainer = document.createElement('div');
    toastContainer.innerHTML = `
        <div class="position-fixed top-0 start-50 translate-middle-x p-3" style="z-index: 1055;">
            <div id="importSuccessToast" class="toast align-items-center text-white bg-success border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <div class="d-flex align-items-center">
                            <i class="bi bi-check-circle-fill me-2"></i>
                            <div>
                                <strong>导入成功！</strong><br>
                                <small>总计处理：${total} 条 | 新增：${added} 条 | 更新：${updated} 条 | 跳过：${skipped} 条</small>
                            </div>
                        </div>
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(toastContainer);

    // 显示Toast提示
    const toast = new bootstrap.Toast(document.getElementById('importSuccessToast'));
    toast.show();

    // Toast消失后移除元素
    document.getElementById('importSuccessToast').addEventListener('hidden.bs.toast', function () {
        toastContainer.remove();
    });
}

// 执行导入
async function performImport() {
    const fileInput = document.getElementById('importFile');
    const file = fileInput.files[0];
    const skipDuplicates = document.getElementById('skipDuplicates').checked;
    const updateExisting = document.getElementById('updateExisting').checked;

    if (!file) {
        alert('请选择要导入的文件');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('skipDuplicates', skipDuplicates);
    formData.append('updateExisting', updateExisting);

    try {
        const response = await axios.post('/users/import', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });

        // 检查响应数据结构
        const result = response.data.data || response.data;
        const added = result.added || 0;
        const updated = result.updated || 0;
        const skipped = result.skipped || 0;
        const total = result.total || 0;
        
        // 显示导入成功提示
        showImportSuccessToast(added, updated, skipped, total);
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('importModal'));
        modal.hide();

        // 刷新用户列表
        loadUsers(currentPage);

    } catch (error) {
        console.error('导入失败:', error);
        alert('导入失败: ' + (error.response?.data?.message || error.message));
    }
}

// 添加CSS样式
const additionalStyles = `
    .user-avatar-large {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 2rem;
    margin: 0 auto;
}

    .profile-avatar {
    position: relative;
}

    .selected-users {
    max-height: 150px;
    overflow-y: auto;
    border: 1px solid #dee2e6;
    padding: 10px;
    border-radius: 5px;
    background-color: #f8f9fa;
}
    `;

// 添加样式到页面
const styleSheet = document.createElement('style');
styleSheet.textContent = additionalStyles;
document.head.appendChild(styleSheet);

async function editUser(id) {
    try {
        const response = await axios.get(`/users/${id}`);
        const user = response.data.data;

        // 检查元素是否存在
        const usernameInput = document.getElementById('username');
        const nameInput = document.getElementById('name');
        const ageInput = document.getElementById('age');
        const userIdInput = document.getElementById('editUserId');
        const modalLabel = document.getElementById('userModalLabel');
        const updateTime=document.getElementById('updatetime');
        if (!usernameInput || !nameInput || !ageInput || !modalLabel || !userIdInput) {
            console.error('编辑表单元素未找到！');
            return;
        }

        // 填充表单
        usernameInput.value = user.username;
        nameInput.value = user.name;
        ageInput.value = user.age;
        userIdInput.value = user.id;
        updateTime.value=user.updateTime;
        modalLabel.textContent = '编辑用户';

        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('editModal'));
        modal.show();
    } catch (error) {
        console.error('获取用户信息失败:', error);
        alert('获取用户信息失败，请重试');
    }
}


//条件查询
function searchUsers() {
    const keyword = document.getElementById('searchInput').value.trim(); // 去掉空格
    // 调用 loadUsers 加载第一页数据，并传入搜索关键字
    loadUsers(1, keyword);
}

//更新后保存
async function saveUser() {
    try {
        // 获取表单数据
        const id = document.getElementById('editUserId').value;
        const username = document.getElementById('username').value.trim();
        const name = document.getElementById('name').value.trim();
        const age = parseInt(document.getElementById('age').value);
        const updateTime = document.getElementById('updatetime').value.trim();

        if (!username || !name || isNaN(age)) {
            alert('请填写完整的用户信息');
            return;
        }

        const userData = { id, username, name, age, updateTime };

        // 调用后端更新接口
        const response = await axios.put(`/users`, userData);

        if (response.data.code === 1) {
            alert('用户更新成功');
            // 隐藏模态框
            const modal = bootstrap.Modal.getInstance(document.getElementById('editModal'));
            modal.hide();
            // 刷新用户表格
            loadUsers(currentPage);
        } else {
            alert('更新失败: ' + response.data.message);
        }
    } catch (error) {
        console.error('更新用户失败:', error);
        alert('更新用户失败: ' + (error.response?.data?.message || error.message));
    }
}
