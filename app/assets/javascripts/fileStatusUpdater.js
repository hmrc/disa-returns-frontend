function initFileStatusUpdater(statusUrl) {
    async function fetchAndUpdate() {
        try {
            const response = await fetch(statusUrl);
            if (!response.ok) return;
            const statuses = await response.json();

            const listEl = document.getElementById("file-status-list");
            if (!listEl) return;

            // Clear the list
            listEl.innerHTML = "";

            // Re-render each file status
            statuses.forEach(s => {
                const li = document.createElement("li");
                li.style.display = "flex";
                li.style.alignItems = "center";
                li.style.justifyContent = "space-between";
                li.style.borderBottom = "1px solid #b1b4b6";
                li.style.padding = "0.5em 0";

                // File name
                const nameDiv = document.createElement("div");
                nameDiv.style.flex = "1";
                if (s._type === "InProgress") {
                    nameDiv.innerHTML = "<strong>file uploading......</strong>";
                } else if (s._type === "Failed") {
                    nameDiv.innerText = "Unknown file";
                } else if (s._type === "UploadedSuccessfully") {
                    nameDiv.innerHTML = `<a href="#" class="govuk-link">${s.name}</a>`;
                }
                li.appendChild(nameDiv);

                // Status badge
                const statusDiv = document.createElement("div");
                if (s._type === "InProgress") {
                    statusDiv.innerHTML = `<span style="background-color: #fff2cc; color: #594d00; padding: 0.2em 0.5em; border-radius: 4px;">Uploading</span>`;
                } else if (s._type === "Failed") {
                    statusDiv.innerHTML = `<span style="background-color: #f8d7da; color: #842029; padding: 0.2em 0.5em; border-radius: 4px;">Rejected</span>`;
                } else if (s._type === "UploadedSuccessfully") {
                    statusDiv.innerHTML = `<span style="background-color: #d1e7dd; color: #0f5132; padding: 0.2em 0.5em; border-radius: 4px;">Uploaded</span>`;
                }
                li.appendChild(statusDiv);

                // Remove link
                const removeDiv = document.createElement("div");
                removeDiv.style.marginLeft = "1em";
                if (s._type === "UploadedSuccessfully") {
                    removeDiv.innerHTML = `<a class="govuk-link" href="/remove/${s.name}">Remove<span class="govuk-visually-hidden"> ${s.name}</span></a>`;
                }
                li.appendChild(removeDiv);

                listEl.appendChild(li);
            });

        } catch (err) {
            console.error("Error updating file statuses:", err);
        }
    }

    // Poll every 5 seconds
    fetchAndUpdate(); // initial load
    setInterval(fetchAndUpdate, 50000);
}
