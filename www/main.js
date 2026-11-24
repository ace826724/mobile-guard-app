
"use strict";

/*
 * MGA - Mobile Guard App
 * Main JavaScript Controller
 * Version: 1.0.0
 *
 * Bu dosya arayüz işlemlerini yönetir.
 * VPN bağlantısı henüz sonraki geliştirme aşamasında
 * Kotlin VPNService ile entegre edilecektir.
 */

const MGA = {
  version: "1.0.0",

  state: {
    protectionActive: false,
    blockedCount: 0,
    checkedCount: 0
  },

  elements: {},

  init() {
    this.cacheElements();
    this.bindEvents();
    this.updateInterface();

    console.log("MGA Mobile Guard App initialized.");
  },

  cacheElements() {
    this.elements = {
      protectionButton: document.getElementById("protectionButton"),
      protectionStatus: document.getElementById("protectionStatus"),
      protectionDescription: document.getElementById("protectionDescription"),
      shieldIcon: document.getElementById("shieldIcon"),
      permissionNote: document.getElementById("permissionNote"),

      blockedCount: document.getElementById("blockedCount"),
      checkedCount: document.getElementById("checkedCount"),

      logsButton: document.getElementById("logsButton"),
      allowlistButton: document.getElementById("allowlistButton"),
      settingsButton: document.getElementById("settingsButton")
    };
  },

  bindEvents() {
    this.elements.protectionButton.addEventListener(
      "click",
      () => this.toggleProtection()
    );

    this.elements.logsButton.addEventListener(
      "click",
      () => this.showComingSoon("Engelleme kayıtları")
    );

    this.elements.allowlistButton.addEventListener(
      "click",
      () => this.showComingSoon("Güvenilir siteler")
    );

    this.elements.settingsButton.addEventListener(
      "click",
      () => this.showComingSoon("Ayarlar")
    );
  },

  toggleProtection() {
    if (this.state.protectionActive) {
      this.stopProtection();
      return;
    }

    this.showMessage(
      "VPN koruma motoru henüz entegre edilmedi. " +
      "Android VPNService bağlantısı sonraki geliştirme aşamasında eklenecek."
    );
  },

  startProtection() {
    /*
     * İleride burada Android Kotlin VPNService başlatılacak.
     * Gerçek servis onayı alınmadan koruma aktif gösterilmemelidir.
     */
    console.warn("MGA VPNService is not integrated yet.");
  },

  stopProtection() {
    /*
     * İleride Android VPNService durdurma işlemi burada yapılacak.
     */
    console.warn("MGA VPNService is not integrated yet.");
  },

  updateInterface() {
    this.elements.blockedCount.textContent =
      this.state.blockedCount;

    this.elements.checkedCount.textContent =
      this.state.checkedCount;

    if (this.state.protectionActive) {
      this.elements.protectionStatus.textContent = "Koruma aktif";
      this.elements.protectionDescription.textContent =
        "MGA cihazını koruyor.";

      this.elements.protectionButton.textContent =
        "Korumayı durdur";

      this.elements.shieldIcon.textContent = "🛡️";
    } else {
      this.elements.protectionStatus.textContent =
        "Henüz başlatılmadı";

      this.elements.protectionDescription.textContent =
        "Koruma motoru henüz etkin değil.";

      this.elements.protectionButton.textContent =
        "Korumayı başlat";

      this.elements.shieldIcon.textContent = "🛡️";
    }
  },

  showComingSoon(feature) {
    this.showMessage(
      feature + " özelliği henüz geliştirme aşamasında."
    );
  },

  showMessage(message) {
    window.alert(message);
  }
};

document.addEventListener("DOMContentLoaded", () => {
  MGA.init();
});
