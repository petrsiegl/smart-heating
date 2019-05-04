Pro spuštění aplikace Smart-Heating.jar proveďte následující:
1)  Překopírujte obsah této složky na Raspberry Pi.

2)  Ujistěte se, že máte nainstalovanou Javu verze alespoň 8.

3)  Přejděte do překopírované složky a spusťte aplikaci příkazem:
            java -jar Smart-Heating.jar
    Zde můžou nastat problémy s oprávněními k přístupu k periferiím
    (bližší informace viz dokumentace projektu).
    Pokud se tak stane, můžete aplikaci spustit s administrátorskými
    právy pomocí:
            sudo java -jar Smart-Heating.jar

4)  Po prvním spuštění se vytvoří .INI soubory,
    ve kterých můžete nastavovat některé vlastnosti aplikace.
    Bližší informace jsou dostupné v uživatelské dokumentaci.

5)  Pokud změníte nastavení vypněte a zapněte aplikaci,
    stejně jako při prvním spuštění.

6) Nyní se již můžete k aplikaci připojit.
