/* ==============================
   TOAST
============================== */

function showMessage(message) {

    let toast = document.getElementById("toast");

    if (!toast) {
        return;
    }

    toast.innerText = message;
    toast.classList.add("show");

    setTimeout(() => {
        toast.classList.remove("show");
    }, 3000);
}


/* ==============================
   SOS
============================== */

let selectedEmergency = "";

function selectEmergency(type) {

    selectedEmergency = type;

    const element =
        document.getElementById("selectedType");

    if (element) {
        element.innerText =
            "Selected Emergency: " + type;
    }
}


function sendSOS() {

    if (!selectedEmergency) {

        showMessage(
            "⚠ Please select an emergency type first."
        );

        return;
    }

    const confirmation = confirm(
        "Send " +
        selectedEmergency +
        " SOS to nearby mesh nodes?"
    );

    if (confirmation) {

        showMessage(
            "🚨 " +
            selectedEmergency +
            " SOS broadcast successfully!"
        );

        console.log(
            "SOS transmitted:",
            selectedEmergency
        );
    }
}


/* ==============================
   QUICK MESH REPLIES
============================== */

function sendQuickReply(message) {

    showMessage(
        "📡 Message sent through mesh: " +
        message
    );

}


/* ==============================
   INCIDENT FILTER
============================== */

function filterIncidents(type) {

    const incidents =
        document.querySelectorAll(".incident");

    incidents.forEach(incident => {

        if (
            type === "all" ||
            incident.classList.contains(type)
        ) {
            incident.style.display = "block";
        } else {
            incident.style.display = "none";
        }

    });

}


/* ==============================
   RESPONSE STATUS
============================== */

function advanceResponse(button) {

    showMessage(
        "🚑 Response status updated."
    );

}


/* ==============================
   LIVE DASHBOARD NUMBERS
============================== */

let nodes = 128;

setInterval(() => {

    const nodeElement =
        document.getElementById("nodes");

    if (!nodeElement) {
        return;
    }

    let change =
        Math.random() > .5 ? 1 : -1;

    nodes += change;

    if (nodes < 120) {
        nodes = 120;
    }

    nodeElement.innerText = nodes;

}, 5000);


/* ==============================
   WELCOME
============================== */

window.addEventListener("load", () => {

    if (window.location.pathname.endsWith("index.html") ||
        window.location.pathname.endsWith("/")) {

        setTimeout(() => {

            showMessage(
                "📡 Community Mesh system is online."
            );

        }, 1000);

    }

});

/* ================= PROFILE MENU + DEMO ACCOUNT SESSION ================= */
function toggleProfileMenu(event){
  event.stopPropagation();
  const menu=document.getElementById("profileDropdown");
  const trigger=document.querySelector(".profile-trigger");
  if(!menu)return;
  const open=menu.classList.toggle("open");
  if(trigger)trigger.setAttribute("aria-expanded",String(open));
}
document.addEventListener("click",function(event){
  const menu=document.getElementById("profileDropdown");
  const wrap=document.querySelector(".profile-menu");
  if(menu&&wrap&&!wrap.contains(event.target)){
    menu.classList.remove("open");
    const trigger=wrap.querySelector(".profile-trigger");
    if(trigger)trigger.setAttribute("aria-expanded","false");
  }
});
function logoutUser(){
  localStorage.removeItem("communityMeshCurrentUser");
  showMessage("You have been logged out.");
  setTimeout(()=>location.href="index.html",450);
}
function refreshProfileMenu(){
  const email=localStorage.getItem("communityMeshCurrentUser");
  const users=JSON.parse(localStorage.getItem("communityMeshUsers")||"{}");
  const user=email?users[email]:null;
  const name=user?`${user.firstName||""} ${user.lastName||""}`.trim():"My Account";
  const nameEl=document.getElementById("menuUserName");
  const emailEl=document.getElementById("menuUserEmail");
  if(nameEl)nameEl.textContent=user?(name||"My Account"):"My Account";
  if(emailEl)emailEl.textContent=user?user.email:"Not logged in";
  document.querySelectorAll('[data-auth-link="login"],[data-auth-link="signup"]').forEach(el=>el.style.display=user?"none":"block");
  const logout=document.getElementById("logoutButton");
  if(logout)logout.style.display=user?"block":"none";
  const triggerLabel=document.querySelector(".profile-trigger-label");
  if(triggerLabel)triggerLabel.textContent=user?(user.firstName||"Profile"):"Account";
  const pName=document.getElementById("profileName");
  const pStatus=document.getElementById("profileStatus");
  const fullName=document.getElementById("profileFullName");
  const pEmail=document.getElementById("profileEmail");
  if(pName)pName.textContent=user?(name||"My Account"):"Guest User";
  if(pStatus)pStatus.textContent=user?"You are logged in.":"You are not logged in.";
  if(fullName)fullName.textContent=user?(name||"—"):"—";
  if(pEmail)pEmail.textContent=user?user.email:"—";
  const profileLogout=document.getElementById("profileLogout");
  if(profileLogout)profileLogout.style.display=user?"inline-block":"none";
}
document.addEventListener("DOMContentLoaded",refreshProfileMenu);
