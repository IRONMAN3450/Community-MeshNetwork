/* ==============================
   Community Mesh shared client
============================== */

const AUTH_TOKEN_KEY = "communityMeshAuthToken";
const CURRENT_USER_KEY = "communityMeshCurrentUser";
const USERS_CACHE_KEY = "communityMeshUsers";

function showMessage(message) {
    let toast = document.getElementById("toast");
    if (!toast) {
        return;
    }
    toast.innerText = message;
    toast.classList.add("show");
    setTimeout(() => toast.classList.remove("show"), 3000);
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;"
    })[character]);
}

function getAuthToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY) || "";
}

function getCachedUser() {
    try {
        const email = localStorage.getItem(CURRENT_USER_KEY);
        const users = JSON.parse(localStorage.getItem(USERS_CACHE_KEY) || "{}");
        return email ? users[email] || null : null;
    } catch (error) {
        return null;
    }
}

function storeSession(account) {
    if (!account || !account.email) {
        return;
    }
    const users = JSON.parse(localStorage.getItem(USERS_CACHE_KEY) || "{}");
    users[account.email] = account;
    localStorage.setItem(USERS_CACHE_KEY, JSON.stringify(users));
    localStorage.setItem(CURRENT_USER_KEY, account.email);
    if (account.token) {
        localStorage.setItem(AUTH_TOKEN_KEY, account.token);
    }
}

function clearSession() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(CURRENT_USER_KEY);
}

async function apiRequest(path, options = {}) {
    const headers = Object.assign(
        { "Content-Type": "application/json" },
        options.headers || {}
    );
    const token = getAuthToken();
    if (token) {
        headers["X-Auth-Token"] = token;
    }

    const response = await fetch(path, Object.assign({}, options, { headers }));
    let body = null;
    const text = await response.text();
    if (text) {
        try {
            body = JSON.parse(text);
        } catch (error) {
            body = { message: text };
        }
    }

    if (!response.ok) {
        const message = (body && body.message) || "Request failed.";
        const error = new Error(message);
        error.status = response.status;
        error.body = body;
        throw error;
    }
    return body;
}

function severityClass(severity) {
    const value = (severity || "HIGH").toLowerCase();
    if (value === "critical") return "critical";
    if (value === "medium" || value === "low") return "medium";
    return "high";
}

function typeClass(type) {
    const value = (type || "").toLowerCase();
    if (value.includes("flood")) return "flood";
    if (value.includes("earth")) return "earthquake";
    if (value.includes("fire")) return "fire";
    if (value.includes("cyclone") || value.includes("rain")) return "cyclone";
    return "flood";
}

function typeEmoji(type) {
    const value = (type || "").toLowerCase();
    if (value.includes("flood")) return "🌊";
    if (value.includes("earth")) return "🌍";
    if (value.includes("fire")) return "🔥";
    if (value.includes("cyclone") || value.includes("rain")) return "🌀";
    if (value.includes("medical")) return "🏥";
    if (value.includes("trap")) return "🧱";
    if (value.includes("evac")) return "🚨";
    return "⚠";
}

function formatStatus(status) {
    return (status || "").replaceAll("_", " ");
}

function pipelineHtml(status) {
    const order = ["RECEIVED", "ASSIGNED", "IN_PROGRESS", "RESOLVED"];
    const current = (status || "RECEIVED").toUpperCase();
    const currentIndex = Math.max(order.indexOf(current), 0);
    const labels = {
        RECEIVED: "Received",
        ASSIGNED: "Assigned",
        IN_PROGRESS: "In Progress",
        RESOLVED: "Resolved"
    };
    return order.map((step, index) => {
        let cls = "";
        let prefix = "○ ";
        if (index < currentIndex || current === "RESOLVED") {
            cls = "done";
            prefix = "✓ ";
        } else if (index === currentIndex) {
            cls = "active-step";
            prefix = "● ";
        }
        return `<span class="${cls}">${prefix}${labels[step]}</span>`;
    }).join("");
}

function requireLogin(message) {
    if (!getAuthToken()) {
        showMessage(message || "Please log in first.");
        setTimeout(() => {
            location.href = "login.html";
        }, 700);
        return false;
    }
    return true;
}

function hasResponderRole() {
    const role = getCachedUser()?.role;
    return role === "RESPONDER" || role === "ADMIN";
}

/* ==============================
   SOS helpers used by sos.html
============================== */

let selectedEmergency = "";

function selectEmergency(type) {
    selectedEmergency = type;
    const element = document.getElementById("selectedType");
    if (element) {
        element.innerText = "Selected Emergency: " + type;
    }
}

async function sendSOS() {
    if (!requireLogin("Please log in before sending an SOS.")) {
        return;
    }
    if (!selectedEmergency) {
        showMessage("⚠ Please select an emergency type first.");
        return;
    }

    const confirmation = confirm("Send " + selectedEmergency + " SOS to nearby mesh nodes?");
    if (!confirmation) {
        return;
    }

    const payload = {
        category: selectedEmergency,
        description: selectedEmergency + " emergency reported via Community Mesh SOS.",
        severity: selectedEmergency === "Trapped" ? "CRITICAL" : "HIGH",
        locationLabel: "Detected device location"
    };
    const incidentId = Number(document.getElementById("sosIncidentSelect")?.value);
    if (incidentId > 0) payload.incidentId = incidentId;

    const submit = async (latitude, longitude) => {
        if (latitude != null && longitude != null) {
            payload.latitude = latitude;
            payload.longitude = longitude;
        }
        try {
            const alert = await apiRequest("/api/sos", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showMessage("🚨 SOS " + alert.publicCode + " broadcast successfully!");
            if (typeof loadMySos === "function") {
                loadMySos();
            }
        } catch (error) {
            showMessage(error.message || "Could not send SOS.");
        }
    };

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => submit(position.coords.latitude, position.coords.longitude),
            () => submit(null, null),
            { timeout: 5000 }
        );
    } else {
        await submit(null, null);
    }
}

async function sendQuickReply(message) {
    try {
        await apiRequest("/api/mesh/messages", {
            method: "POST",
            body: JSON.stringify({ content: message })
        });
        showMessage("📡 Message sent through mesh: " + message);
    } catch (error) {
        showMessage(error.message || "Could not send mesh message.");
    }
}

function filterIncidents(type) {
    const incidents = document.querySelectorAll(".incident");
    incidents.forEach((incident) => {
        if (type === "all" || incident.classList.contains(type)) {
            incident.style.display = "block";
        } else {
            incident.style.display = "none";
        }
    });
}

async function advanceResponse(button) {
    const assignmentId = button && button.dataset ? button.dataset.assignmentId : null;
    if (!assignmentId) {
        showMessage("No assignment selected.");
        return;
    }
    if (!requireLogin("Responder login required to update status.")) {
        return;
    }
    try {
        await apiRequest("/api/rescue/assignments/" + assignmentId + "/advance", { method: "POST", body: "{}" });
        showMessage("🚑 Response status updated.");
        if (typeof loadRescueTracking === "function") {
            loadRescueTracking();
        } else {
            setTimeout(() => location.reload(), 500);
        }
    } catch (error) {
        showMessage(error.message || "Could not update response status.");
    }
}

/* ================= PROFILE MENU ================= */

function toggleProfileMenu(event) {
    event.stopPropagation();
    const menu = document.getElementById("profileDropdown");
    const trigger = event.currentTarget || document.querySelector(".profile-trigger");
    if (!menu) return;
    const open = menu.classList.toggle("open");
    if (trigger) trigger.setAttribute("aria-expanded", String(open));
}

document.addEventListener("click", function (event) {
    const menu = document.getElementById("profileDropdown");
    const wrap = document.querySelector(".profile-menu");
    if (menu && wrap && !wrap.contains(event.target)) {
        menu.classList.remove("open");
        const trigger = wrap.querySelector(".profile-trigger");
        if (trigger) trigger.setAttribute("aria-expanded", "false");
    }
});

document.addEventListener("keydown", function (event) {
    if (event.key !== "Escape") return;
    const menu = document.getElementById("profileDropdown");
    const trigger = document.querySelector(".profile-trigger");
    if (menu) menu.classList.remove("open");
    if (trigger) trigger.setAttribute("aria-expanded", "false");
});

async function logoutUser() {
    try {
        if (getAuthToken()) {
            await apiRequest("/api/accounts/logout", { method: "POST", body: "{}" });
        }
    } catch (error) {
        // Clear local session even if the server call fails.
    }
    clearSession();
    refreshProfileMenu();
    const menu = document.getElementById("profileDropdown");
    const trigger = document.querySelector(".profile-trigger");
    if (menu) menu.classList.remove("open");
    if (trigger) trigger.setAttribute("aria-expanded", "false");
    showMessage("You have been logged out.");
    const path = window.location.pathname;
    if (!path.endsWith("index.html") && path !== "/") {
        setTimeout(() => location.href = "index.html", 450);
    }
}

function applyUserToProfileUi(user) {
    const name = user ? `${user.firstName || ""} ${user.lastName || ""}`.trim() : "My Account";
    const nameEl = document.getElementById("menuUserName");
    const emailEl = document.getElementById("menuUserEmail");
    if (nameEl) nameEl.textContent = user ? (name || "My Account") : "My Account";
    if (emailEl) emailEl.textContent = user ? user.email : "Not logged in";

    document.querySelectorAll('[data-auth-link="login"],[data-auth-link="signup"]').forEach((el) => {
        el.style.display = user ? "none" : "block";
    });
    const logout = document.getElementById("logoutButton");
    if (logout) logout.style.display = user ? "block" : "none";

    const triggerLabel = document.querySelector(".profile-trigger-label");
    if (triggerLabel) triggerLabel.textContent = user ? (user.firstName || "Profile") : "Account";

    const pName = document.getElementById("profileName");
    const pStatus = document.getElementById("profileStatus");
    const fullName = document.getElementById("profileFullName");
    const pEmail = document.getElementById("profileEmail");
    const pPhone = document.getElementById("profilePhone");
    const pRole = document.getElementById("profileRole");
    const pEmergencyName = document.getElementById("profileEmergencyName");
    const pEmergencyPhone = document.getElementById("profileEmergencyPhone");
    const pLocation = document.getElementById("profileLocation");

    if (pName) pName.textContent = user ? (name || "My Account") : "Guest User";
    if (pStatus) pStatus.textContent = user ? "You are logged in." : "You are not logged in.";
    if (fullName) fullName.textContent = user ? (name || "—") : "—";
    if (pEmail) pEmail.textContent = user ? user.email : "—";
    if (pPhone) pPhone.textContent = user && user.phone ? user.phone : "—";
    if (pRole) pRole.textContent = user ? ((user.role || user.userType || "CITIZEN")) : "—";
    if (pEmergencyName) pEmergencyName.textContent = user && user.emergencyContactName ? user.emergencyContactName : "—";
    if (pEmergencyPhone) pEmergencyPhone.textContent = user && user.emergencyContactPhone ? user.emergencyContactPhone : "—";
    if (pLocation) {
        pLocation.textContent = user && user.latitude != null && user.longitude != null
            ? `${Number(user.latitude).toFixed(5)}, ${Number(user.longitude).toFixed(5)}`
            : "Not shared";
    }
    const latitudeInput = document.getElementById("profileLatitude");
    const longitudeInput = document.getElementById("profileLongitude");
    if (latitudeInput && user && user.latitude != null) latitudeInput.value = user.latitude;
    if (longitudeInput && user && user.longitude != null) longitudeInput.value = user.longitude;

    const profileLogout = document.getElementById("profileLogout");
    if (profileLogout) profileLogout.style.display = user ? "inline-block" : "none";
    const profileLogin = document.getElementById("profileLogin");
    if (profileLogin) profileLogin.style.display = user ? "none" : "inline-block";
    const profileEdit = document.getElementById("profileEditPanel");
    if (profileEdit) profileEdit.style.display = user ? "block" : "none";

    if (user) {
        const firstName = document.getElementById("editFirstName");
        const lastName = document.getElementById("editLastName");
        const phone = document.getElementById("editPhone");
        const emergencyName = document.getElementById("editEmergencyName");
        const emergencyPhone = document.getElementById("editEmergencyPhone");
        if (firstName) firstName.value = user.firstName || "";
        if (lastName) lastName.value = user.lastName || "";
        if (phone) phone.value = user.phone || "";
        if (emergencyName) emergencyName.value = user.emergencyContactName || "";
        if (emergencyPhone) emergencyPhone.value = user.emergencyContactPhone || "";
    }
}

function refreshProfileMenu() {
    applyUserToProfileUi(getCachedUser());
}

async function refreshProfileFromServer() {
    if (!getAuthToken()) {
        refreshProfileMenu();
        return;
    }
    try {
        const account = await apiRequest("/api/accounts/me");
        storeSession(account);
        applyUserToProfileUi(account);
    } catch (error) {
        if (error.status === 401) {
            clearSession();
        }
        refreshProfileMenu();
    }
}

async function saveProfile(event) {
    if (event) event.preventDefault();
    if (!requireLogin()) return;
    const payload = {
        firstName: document.getElementById("editFirstName").value.trim(),
        lastName: document.getElementById("editLastName").value.trim(),
        phone: document.getElementById("editPhone").value.trim(),
        emergencyContactName: document.getElementById("editEmergencyName").value.trim(),
        emergencyContactPhone: document.getElementById("editEmergencyPhone").value.trim()
    };
    try {
        const account = await apiRequest("/api/accounts/me", {
            method: "PUT",
            body: JSON.stringify(payload)
        });
        storeSession(account);
        applyUserToProfileUi(account);
        showMessage("Profile updated successfully.");
    } catch (error) {
        showMessage(error.message || "Could not update profile.");
    }
}

function updateProfileLocation() {
    if (!requireLogin("Please log in before sharing your location.")) return;
    const button = document.getElementById("profileLocationButton");
    const status = document.getElementById("profileLocationStatus");
    const buttonText = "Share current GPS location with your profile";
    const setStatus = (message) => {
        if (status) status.textContent = message;
    };
    const finish = () => {
        if (button) {
            button.disabled = false;
            button.textContent = buttonText;
        }
    };
    if (!navigator.geolocation) {
        const message = !window.isSecureContext
            ? "GPS is blocked because this page is not secure. Use localhost on this computer or configure HTTPS; you can also save coordinates manually below."
            : "GPS is not available in this browser. You can save coordinates manually below.";
        setStatus(message);
        showMessage(message);
        return;
    }
    if (button) {
        button.disabled = true;
        button.textContent = "Getting location...";
    }
    setStatus("Waiting for location permission and GPS...");
    try {
        navigator.geolocation.getCurrentPosition(async (position) => {
            try {
                const account = await apiRequest("/api/accounts/me", {
                    method: "PUT",
                    body: JSON.stringify({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    })
                });
                storeSession(account);
                applyUserToProfileUi(account);
                setStatus("Location saved to your profile.");
                showMessage("Profile location updated.");
            } catch (error) {
                const message = error.message || "Could not save your profile location.";
                setStatus(message);
                showMessage(message);
            } finally {
                finish();
            }
        }, (error) => {
            let message;
            if (error.code === error.PERMISSION_DENIED) {
                message = "Location permission was denied. Allow location access in your browser settings, then try again.";
            } else if (error.code === error.POSITION_UNAVAILABLE) {
                message = "Your device could not determine its location. Check that location services are enabled.";
            } else if (error.code === error.TIMEOUT) {
                message = "Location request timed out. Check your GPS signal and try again.";
            } else {
                message = "Your location could not be determined. Check browser permissions and try again.";
            }
            setStatus(message);
            showMessage(message);
            finish();
        }, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 });
    } catch (error) {
        const message = "The browser could not start location access. Check site permissions and try again.";
        setStatus(message);
        showMessage(message);
        finish();
    }
}

async function saveProfileLocation(event) {
    if (event) event.preventDefault();
    if (!requireLogin("Please log in before saving your location.")) return;

    const latitudeInput = document.getElementById("profileLatitude");
    const longitudeInput = document.getElementById("profileLongitude");
    if (!latitudeInput?.value.trim() || !longitudeInput?.value.trim()) {
        const message = "Enter both latitude and longitude.";
        const status = document.getElementById("profileLocationStatus");
        if (status) status.textContent = message;
        showMessage(message);
        return;
    }
    const latitude = Number(latitudeInput?.value);
    const longitude = Number(longitudeInput?.value);
    if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90
            || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
        const message = "Enter a valid latitude (-90 to 90) and longitude (-180 to 180).";
        const status = document.getElementById("profileLocationStatus");
        if (status) status.textContent = message;
        showMessage(message);
        return;
    }

    const button = document.getElementById("profileManualLocationButton");
    const status = document.getElementById("profileLocationStatus");
    if (button) {
        button.disabled = true;
        button.textContent = "Saving coordinates...";
    }
    try {
        const account = await apiRequest("/api/accounts/me", {
            method: "PUT",
            body: JSON.stringify({ latitude, longitude })
        });
        storeSession(account);
        applyUserToProfileUi(account);
        if (status) status.textContent = "Coordinates saved to your profile.";
        showMessage("Profile location updated.");
    } catch (error) {
        const message = error.message || "Could not save your profile location.";
        if (status) status.textContent = message;
        showMessage(message);
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = "Save coordinates";
        }
    }
}

/* ==============================
   Page loaders
============================== */

let dashboardLoadInProgress = false;
let dashboardRefreshTimer;

async function loadDashboard() {
    const incidentsEl = document.getElementById("incidents");
    if (!incidentsEl && !document.getElementById("sosCount")) {
        return;
    }
    if (dashboardLoadInProgress) return;
    dashboardLoadInProgress = true;
    try {
        const stats = await apiRequest("/api/dashboard");
        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.textContent = value;
        };
        setText("incidents", stats.activeIncidents ?? 0);
        setText("sosCount", stats.activeSos ?? 0);
        setText("nodes", stats.activeNodes ?? 0);
        setText("teamsDeployed", stats.teamsDeployed ?? 0);
        setText("statReceived", stats.received ?? 0);
        setText("statAssigned", stats.assigned ?? 0);
        setText("statInProgress", stats.inProgress ?? 0);
        setText("statResolved", stats.resolved ?? 0);

        const grid = document.getElementById("dashboardIncidentGrid");
        if (grid && Array.isArray(stats.recentIncidents)) {
            grid.innerHTML = stats.recentIncidents.map((incident) => `
                <div class="incident-card ${typeClass(incident.type)}">
                    <span class="severity ${severityClass(incident.severity)}">${escapeHtml(incident.severity)}</span>
                    <h3>${typeEmoji(incident.type)} ${escapeHtml(incident.title)}</h3>
                    <p>${escapeHtml(incident.description || incident.sector)}</p>
                    <strong>${incident.activeSosCount || 0} SOS requests</strong>
                </div>
            `).join("") || "<p>No incidents yet.</p>";
        }
        const sosGrid = document.getElementById("dashboardSosGrid");
        if (sosGrid && Array.isArray(stats.recentSosAlerts)) {
            sosGrid.innerHTML = stats.recentSosAlerts.map((alert) => `
                <div class="incident-card ${typeClass(alert.category)}">
                    <span class="severity ${severityClass(alert.severity)}">${escapeHtml(alert.severity)}</span>
                    <h3>🆘 ${escapeHtml(alert.publicCode)} — ${escapeHtml(alert.category)}</h3>
                    <p>Reported by ${escapeHtml(alert.reporterName || "Community member")}</p>
                    <p>${escapeHtml(alert.description || alert.locationLabel || "Location not provided")}</p>
                    <p>Status: <strong>${escapeHtml(formatStatus(alert.status))}</strong></p>
                    ${alert.latitude != null && alert.longitude != null
                        ? `<p>Location: ${Number(alert.latitude).toFixed(5)}, ${Number(alert.longitude).toFixed(5)}</p>`
                        : ""}
                </div>
            `).join("") || "<p>No active SOS requests.</p>";
        }
        const updatedAt = document.getElementById("dashboardUpdatedAt");
        if (updatedAt) updatedAt.textContent = `Last updated ${new Date().toLocaleTimeString()}. Refreshes automatically.`;
    } catch (error) {
        const grid = document.getElementById("dashboardIncidentGrid");
        const sosGrid = document.getElementById("dashboardSosGrid");
        if (grid) grid.textContent = "Could not load incidents. " + (error.message || "");
        if (sosGrid) sosGrid.textContent = "Could not load SOS requests. " + (error.message || "");
        showMessage(error.message || "Could not load dashboard.");
    } finally {
        dashboardLoadInProgress = false;
    }
}

async function loadIncidentsPage() {
    const grid = document.getElementById("incidentLiveGrid");
    if (!grid) return;
    if (!grid.dataset.actionsBound) {
        grid.addEventListener("click", async (event) => {
            const button = event.target instanceof Element
                ? event.target.closest("button[data-incident-action]")
                : null;
            if (!button) return;
            const id = button.dataset.incidentId;
            if (!id || !requireLogin("Responder/Admin login required.")) return;
            try {
                if (button.dataset.incidentAction === "archive") {
                    if (!confirm("Archive this incident?")) return;
                    await apiRequest("/api/incidents/" + encodeURIComponent(id), { method: "DELETE" });
                    showMessage("Incident archived.");
                } else {
                    await apiRequest("/api/incidents/" + encodeURIComponent(id), {
                        method: "PUT",
                        body: JSON.stringify({ status: "RESOLVED" })
                    });
                    showMessage("Incident marked resolved.");
                }
                loadIncidentsPage();
                loadDashboard();
            } catch (error) {
                showMessage(error.message || "Could not update incident.");
            }
        });
        grid.dataset.actionsBound = "true";
    }
    try {
        const incidents = await apiRequest("/api/incidents");
        grid.innerHTML = incidents.map((incident) => `
            <div class="incident-card ${typeClass(incident.type)} incident ${typeClass(incident.type)}">
                <span class="severity ${severityClass(incident.severity)}">${escapeHtml(incident.severity)}</span>
                <h3>${typeEmoji(incident.type)} ${escapeHtml(incident.type)}</h3>
                <p>${escapeHtml(incident.sector)}</p>
                <hr>
                <p>Status: <strong>${escapeHtml(incident.status)}</strong></p>
                <p>Active SOS: <strong>${incident.activeSosCount || 0}</strong></p>
                <p>Response teams: <strong>${incident.responseTeamCount || 0}</strong></p>
                <details>
                    <summary>View incident details</summary>
                    <p><strong>${escapeHtml(incident.title)}</strong></p>
                    <p>${escapeHtml(incident.description || "No description provided.")}</p>
                    ${incident.latitude == null || incident.longitude == null ? "" : `<p>Location: ${escapeHtml(incident.latitude)}, ${escapeHtml(incident.longitude)}</p>`}
                </details>
                ${hasResponderRole() && incident.status !== "RESOLVED" && incident.status !== "ARCHIVED"
                    ? `<button type="button" data-incident-action="resolve" data-incident-id="${escapeHtml(incident.id)}">Resolve</button>` : ""}
                ${hasResponderRole() && incident.status !== "ARCHIVED"
                    ? `<button type="button" data-incident-action="archive" data-incident-id="${escapeHtml(incident.id)}">Archive</button>` : ""}
            </div>
        `).join("") || "<p>No incidents available.</p>";
    } catch (error) {
        grid.textContent = error.message || "Could not load incidents.";
    }
}

async function loadRescueTracking() {
    const list = document.getElementById("responseLiveList");
    if (!list) return;
    try {
        const [assignments, stats] = await Promise.all([
            apiRequest("/api/rescue/assignments"),
            apiRequest("/api/dashboard")
        ]);
        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.textContent = value;
        };
        setText("rescueReceived", stats.received ?? 0);
        setText("rescueAssigned", stats.assigned ?? 0);
        setText("rescueInProgress", stats.inProgress ?? 0);
        setText("rescueResolved", stats.resolved ?? 0);

        list.innerHTML = assignments.map((item) => `
            <div class="response-card">
                <div class="response-header">
                    <strong>SOS #${escapeHtml(item.sosPublicCode)}</strong>
                    <span class="severity ${severityClass(item.severity)}">${escapeHtml(item.severity || "HIGH")}</span>
                </div>
                <p>${typeEmoji(item.category)} ${escapeHtml(item.category || "Emergency")}</p>
                <div class="pipeline">${pipelineHtml(item.status)}</div>
                <p class="team">🚑 Assigned Team: ${escapeHtml(item.teamName || "Unassigned")}</p>
                ${item.notes ? `<p>Notes: ${escapeHtml(item.notes)}</p>` : ""}
                ${item.status === "RESOLVED" || item.status === "CANCELLED" ? "" : `<button data-assignment-id="${escapeHtml(item.id)}" onclick="advanceResponse(this)">Update Status</button>`}
            </div>
        `).join("") || "<p>No rescue assignments yet.</p>";
    } catch (error) {
        list.textContent = error.message || "Could not load rescue tracking.";
    }
}

function appendMeshMessage(message, currentUser) {
    const box = document.getElementById("meshMessages");
    if (!box || !message) return;
    const mine = currentUser && message.senderName &&
        (`${currentUser.firstName || ""} ${currentUser.lastName || ""}`.trim() === message.senderName);
    const div = document.createElement("div");
    div.className = "message " + (mine ? "sent" : "received");
    div.innerHTML = `
        <strong>${escapeHtml(message.senderName || "Node")}</strong>
        <p>${escapeHtml(message.content)}</p>
        <small>${escapeHtml(message.deliveryStatus || "STORED")} • ${escapeHtml(message.hopCount || 1)} hops</small>
    `;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

async function loadMeshCommunication() {
    const box = document.getElementById("meshMessages");
    if (!box) return;
    const user = getCachedUser();
    try {
        const messages = await apiRequest("/api/mesh/messages");
        box.innerHTML = "";
        messages.forEach((message) => appendMeshMessage(message, user));

        const status = await apiRequest("/api/mesh/status");
        const online = document.getElementById("meshOnlineStatus");
        if (online) {
            online.textContent = "● " + (status.connectedClients || 0) + " connected";
        }

        const protocol = location.protocol === "https:" ? "wss" : "ws";
        const token = getAuthToken();
        const socket = new WebSocket(`${protocol}://${location.host}/ws/mesh${token ? ("?token=" + encodeURIComponent(token)) : ""}`);
        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.type === "mesh-message") {
                    appendMeshMessage(data.payload, getCachedUser());
                }
            } catch (error) {
                // Ignore malformed frames.
            }
        };
        window.__meshSocket = socket;
    } catch (error) {
        box.innerHTML = "<p>Could not load mesh messages.</p>";
    }
}

async function sendMeshChat(event) {
    if (event) event.preventDefault();
    const input = document.getElementById("meshInput");
    if (!input) return;
    const content = input.value.trim();
    if (!content) return;
    try {
        if (window.__meshSocket && window.__meshSocket.readyState === WebSocket.OPEN) {
            window.__meshSocket.send(JSON.stringify({
                type: "send",
                token: getAuthToken(),
                content
            }));
        } else {
            await apiRequest("/api/mesh/messages", {
                method: "POST",
                body: JSON.stringify({ content })
            });
        }
        input.value = "";
    } catch (error) {
        showMessage(error.message || "Could not send message.");
    }
}

async function loadNetworkMapData(map, createMarkerIcon) {
    try {
        const data = await apiRequest("/api/map");
        window.__meshMarkerLayer = window.__meshMarkerLayer || L.layerGroup().addTo(map);
        window.__meshMarkerLayer.clearLayers();
        (data.sosAlerts || []).forEach((alert) => {
            if (alert.latitude == null || alert.longitude == null) return;
            L.marker([alert.latitude, alert.longitude], {
                icon: createMarkerIcon("#ff5252", "🆘")
            }).addTo(window.__meshMarkerLayer || map).bindPopup(`<strong>${escapeHtml(alert.publicCode)}</strong><br>${escapeHtml(alert.category)}<br>${escapeHtml(alert.status)}`);
        });
        (data.teams || []).forEach((team) => {
            if (team.latitude == null || team.longitude == null) return;
            L.marker([team.latitude, team.longitude], {
                icon: createMarkerIcon("#00e676", "🚑")
            }).addTo(window.__meshMarkerLayer || map).bindPopup(`<strong>${escapeHtml(team.name)}</strong><br>${escapeHtml(team.specialty)}<br>${escapeHtml(team.status)}`);
        });
        (data.nodes || []).forEach((node) => {
            if (node.latitude == null || node.longitude == null) return;
            L.marker([node.latitude, node.longitude], {
                icon: createMarkerIcon("#00d9ff", "📡")
            }).addTo(window.__meshMarkerLayer || map).bindPopup(`<strong>${escapeHtml(node.displayName)}</strong><br>${escapeHtml(node.nodeType)}<br>${escapeHtml(node.status)}`);
        });
        (data.incidents || []).forEach((incident) => {
            if (incident.latitude == null || incident.longitude == null) return;
            L.circleMarker([incident.latitude, incident.longitude], {
                radius: 18,
                color: "#ffb300",
                fillColor: "#ffb300",
                fillOpacity: 0.2
            }).addTo(window.__meshMarkerLayer || map).bindPopup(`<strong>${escapeHtml(incident.title)}</strong><br>${escapeHtml(incident.status)}`);
        });
    } catch (error) {
        showMessage(error.message || "Could not load map data.");
    }
}

window.addEventListener("load", () => {
    if (window.location.pathname.endsWith("index.html") || window.location.pathname.endsWith("/")) {
        setTimeout(() => showMessage("📡 Community Mesh system is online."), 1000);
    }
});

document.addEventListener("DOMContentLoaded", () => {
    const profileLocationStatus = document.getElementById("profileLocationStatus");
    if (profileLocationStatus && !window.isSecureContext) {
        profileLocationStatus.textContent = "This HTTP address does not allow browser GPS. Use localhost on this computer or HTTPS, or enter coordinates manually below.";
    }
    refreshProfileFromServer();
    loadDashboard();
    loadIncidentsPage();
    loadRescueTracking();
    loadMeshCommunication();
    if (document.getElementById("dashboardIncidentGrid") && !dashboardRefreshTimer) {
        dashboardRefreshTimer = window.setInterval(loadDashboard, 5000);
    }
});
