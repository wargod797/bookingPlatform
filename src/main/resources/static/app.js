const state = {
    theatreId: null,
    movieId: null,
    showId: null,
    bookingId: null,
    browseResults: []
};

const posters = {
    interstellar: "/images/interstellar-poster.svg",
    inception: "/images/inception-poster.svg",
    default: "/images/movie-default.svg"
};

const elements = {
    theatreId: document.getElementById("state-theatre-id"),
    movieId: document.getElementById("state-movie-id"),
    showId: document.getElementById("state-show-id"),
    bookingId: document.getElementById("state-booking-id"),
    scenarioHint: document.getElementById("scenario-hint"),
    browseResults: document.getElementById("browse-results"),
    responseTitle: document.getElementById("response-title"),
    responseStatus: document.getElementById("response-status"),
    responseOutput: document.getElementById("response-output")
};

bindEvents();
renderState();
renderEmptyBrowseBoard();

function bindEvents() {
    document.getElementById("theatre-form").addEventListener("submit", handleTheatreCreate);
    document.getElementById("movie-form").addEventListener("submit", handleMovieCreate);
    document.getElementById("show-form").addEventListener("submit", handleShowCreate);
    document.getElementById("seat-form").addEventListener("submit", handleSeatAllocation);
    document.getElementById("browse-form").addEventListener("submit", handleBrowse);
    document.getElementById("booking-form").addEventListener("submit", handleBookingCreate);
    document.getElementById("booking-lookup-form").addEventListener("submit", handleBookingLookup);
    document.getElementById("movie-list-form").addEventListener("submit", handleMovieList);
    document.getElementById("movie-by-id-form").addEventListener("submit", handleMovieById);

    document.querySelectorAll("[data-preset]").forEach((button) => {
        button.addEventListener("click", () => applyPreset(button.dataset.preset));
    });

    elements.browseResults.addEventListener("click", (event) => {
        const action = event.target.closest("[data-show-id]");
        if (!action) {
            return;
        }

        const showId = toInt(action.dataset.showId);
        state.showId = showId;
        updateInputValue("seat-form", "showId", showId);
        updateInputValue("booking-form", "showId", showId);
        renderState();
        setScenarioHint(`Show ${showId} selected from browse results. Reserve seats when ready.`);
    });
}

async function handleTheatreCreate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
        theatreName: form.get("theatreName").trim(),
        cityName: form.get("cityName").trim()
    };

    const result = await requestJson("POST", "/partners/theatres", payload, "Onboard theatre");
    if (!result.ok) {
        return;
    }

    state.theatreId = result.data.id ?? state.theatreId;
    updateInputValue("show-form", "theatreId", state.theatreId);
    renderState();
}

async function handleMovieCreate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
        title: form.get("title").trim(),
        genre: form.get("genre").trim(),
        language: form.get("language").trim()
    };

    const result = await requestJson("POST", "/movies", payload, "Create movie");
    if (!result.ok) {
        return;
    }

    state.movieId = result.data.id ?? state.movieId;
    updateInputValue("show-form", "movieId", state.movieId);
    updateInputValue("browse-form", "movieId", state.movieId);
    updateInputValue("movie-by-id-form", "movieId", state.movieId);
    renderState();
}

async function handleShowCreate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
        movieId: toInt(form.get("movieId")),
        theatreId: toInt(form.get("theatreId")),
        showDate: form.get("showDate"),
        showTime: form.get("showTime"),
        price: Number.parseFloat(form.get("price"))
    };

    const result = await requestJson("POST", "/partners/shows", payload, "Create show");
    if (!result.ok) {
        return;
    }

    state.showId = result.data.id ?? state.showId;
    updateInputValue("seat-form", "showId", state.showId);
    updateInputValue("booking-form", "showId", state.showId);
    renderState();
}

async function handleSeatAllocation(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const showId = toInt(form.get("showId"));
    const payload = {
        seatNumbers: parseCsvList(form.get("seatNumbers"))
    };

    await requestJson("POST", `/partners/shows/${showId}/seats`, payload, "Allocate seat inventory");
}

async function handleBrowse(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const query = new URLSearchParams({
        movieId: String(toInt(form.get("movieId"))),
        city: form.get("city").trim(),
        date: form.get("date")
    });

    renderBrowseSkeletons();
    const result = await requestJson("GET", `/browse/shows?${query.toString()}`, null, "Browse shows");
    if (!result.ok) {
        renderEmptyBrowseBoard("Browse request failed. Check the response console for details.");
        return;
    }

    state.browseResults = Array.isArray(result.data) ? result.data : [];
    if (state.browseResults.length === 1 && state.browseResults[0].id != null) {
        state.showId = state.browseResults[0].id;
        updateInputValue("seat-form", "showId", state.showId);
        updateInputValue("booking-form", "showId", state.showId);
        renderState();
    }
    renderBrowseResults(state.browseResults);
}

async function handleBookingCreate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
        showId: toInt(form.get("showId")),
        seats: parseCsvList(form.get("seats"))
    };

    const result = await requestJson("POST", "/bookings", payload, "Create booking");
    if (!result.ok) {
        return;
    }

    state.bookingId = result.data.id ?? state.bookingId;
    updateInputValue("booking-lookup-form", "bookingId", state.bookingId);
    renderState();

    if (typeof result.data.totalPrice === "number") {
        setScenarioHint(`Booking completed successfully. Returned total price: ${formatAmount(result.data.totalPrice)}.`);
    }
}

async function handleBookingLookup(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const bookingId = toInt(form.get("bookingId"));

    await requestJson("GET", `/bookings/${bookingId}`, null, "Fetch booking");
}

async function handleMovieList(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    const genre = form.get("genre").trim();
    const language = form.get("language").trim();

    if (genre) {
        params.set("genre", genre);
    }
    if (language) {
        params.set("language", language);
    }

    const path = params.toString() ? `/movies?${params.toString()}` : "/movies";
    await requestJson("GET", path, null, "List movies");
}

async function handleMovieById(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const movieId = toInt(form.get("movieId"));

    await requestJson("GET", `/movies/${movieId}`, null, "Fetch movie by id");
}

function applyPreset(name) {
    if (name === "mumbai") {
        setFormValues("theatre-form", {
            theatreName: "PVR Andheri",
            cityName: "Mumbai"
        });
        setFormValues("movie-form", {
            title: "Interstellar",
            genre: "Sci-Fi",
            language: "English"
        });
        setFormValues("show-form", {
            movieId: state.movieId ?? 1,
            theatreId: state.theatreId ?? 1,
            showDate: "2026-04-04",
            showTime: "13:00",
            price: "100"
        });
        setFormValues("seat-form", {
            showId: state.showId ?? 1,
            seatNumbers: "A1, A2, A3, A4, A5"
        });
        setFormValues("browse-form", {
            movieId: state.movieId ?? 1,
            city: "Mumbai",
            date: "2026-04-04"
        });
        setFormValues("booking-form", {
            showId: state.showId ?? 1,
            seats: "A1, A2, A3"
        });
        setScenarioHint("Mumbai offer scenario loaded. Three seats at 100.0 for 13:00 in PVR Andheri should return 200.0.");
        return;
    }

    setFormValues("theatre-form", {
        theatreName: "Downtown Screens",
        cityName: "Delhi"
    });
    setFormValues("movie-form", {
        title: "Inception",
        genre: "Thriller",
        language: "English"
    });
    setFormValues("show-form", {
        movieId: state.movieId ?? 1,
        theatreId: state.theatreId ?? 2,
        showDate: "2026-04-04",
        showTime: "13:00",
        price: "100"
    });
    setFormValues("seat-form", {
        showId: state.showId ?? 2,
        seatNumbers: "B1, B2, B3, B4"
    });
    setFormValues("browse-form", {
        movieId: state.movieId ?? 1,
        city: "Delhi",
        date: "2026-04-04"
    });
    setFormValues("booking-form", {
        showId: state.showId ?? 2,
        seats: "B1, B2, B3"
    });
    setScenarioHint("Delhi afternoon scenario loaded. Three seats at 100.0 outside the offer location should return 240.0.");
}

async function requestJson(method, path, payload, label) {
    showResponseSkeleton(label);

    try {
        const options = {
            method,
            headers: {}
        };

        if (payload != null) {
            options.headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(payload);
        }

        const response = await fetch(path, options);
        const rawText = await response.text();
        const parsed = tryParseJson(rawText);
        const pretty = parsed == null ? rawText : JSON.stringify(parsed, null, 2);

        if (!response.ok) {
            const message = parsed?.message || parsed?.error || response.statusText || "Request failed";
            renderResponse(`${label} failed`, `${response.status} ${message}`, "error", pretty || message);
            return { ok: false, data: parsed, response };
        }

        renderResponse(label, `${response.status} ${response.statusText}`, "success", pretty || "{}");
        return { ok: true, data: parsed, response };
    } catch (error) {
        renderResponse(
            `${label} failed`,
            "Network error",
            "error",
            JSON.stringify(
                {
                    message: error.message,
                    hint: "Make sure the Spring Boot application is running and accessible at the current host."
                },
                null,
                2
            )
        );
        return { ok: false, error };
    }
}

function renderBrowseResults(results) {
    if (!results.length) {
        renderEmptyBrowseBoard("No matching shows were returned. Create the theatre, movie, show, and seats first.");
        return;
    }

    elements.browseResults.innerHTML = results.map((show) => {
        const title = show.movie?.title ?? "Movie";
        const theatre = show.theatre?.name ?? "Theatre";
        const city = show.theatre?.city?.name ?? "City";
        const poster = resolvePoster(title);
        const price = typeof show.price === "number" ? formatAmount(show.price) : show.price;

        return `
            <article class="show-card">
                <img class="show-poster" src="${poster}" alt="${escapeHtml(title)} poster">
                <div class="show-copy">
                    <div>
                        <h3>${escapeHtml(title)}</h3>
                        <p>${escapeHtml(theatre)} in ${escapeHtml(city)}</p>
                    </div>
                    <div class="show-meta">
                        <span>Show #${escapeHtml(String(show.id ?? "-"))}</span>
                        <span>${escapeHtml(String(show.showDate ?? "-"))}</span>
                        <span>${escapeHtml(String(show.showTime ?? "-"))}</span>
                        <span>${escapeHtml(String(price))}</span>
                    </div>
                    <button type="button" data-show-id="${escapeHtml(String(show.id ?? ""))}">Use this show</button>
                </div>
            </article>
        `;
    }).join("");
}

function renderBrowseSkeletons() {
    elements.browseResults.innerHTML = Array.from({ length: 3 }, () => `
        <article class="skeleton-card">
            <div class="skeleton-poster"></div>
            <div class="skeleton-lines">
                <div class="skeleton-line long"></div>
                <div class="skeleton-line medium"></div>
                <div class="skeleton-line short"></div>
                <div class="skeleton-line medium"></div>
            </div>
        </article>
    `).join("");
}

function renderEmptyBrowseBoard(message = "Browse results will appear here after you query a movie, city, and date.") {
    elements.browseResults.innerHTML = `
        <div class="empty-board">
            <h3>No shows loaded yet</h3>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

function showResponseSkeleton(label) {
    elements.responseTitle.textContent = `${label} in progress`;
    elements.responseStatus.textContent = "Loading";
    elements.responseStatus.className = "response-pill loading";
    elements.responseOutput.innerHTML = `
        <div class="response-skeleton">
            <div class="skeleton-line long"></div>
            <div class="skeleton-line medium"></div>
            <div class="skeleton-line long"></div>
            <div class="skeleton-line medium"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line long"></div>
            <div class="skeleton-line medium"></div>
        </div>
    `;
}

function renderResponse(title, status, tone, body) {
    elements.responseTitle.textContent = title;
    elements.responseStatus.textContent = status;
    elements.responseStatus.className = `response-pill ${tone}`;
    elements.responseOutput.innerHTML = `<pre>${escapeHtml(body)}</pre>`;
}

function renderState() {
    elements.theatreId.textContent = state.theatreId ?? "-";
    elements.movieId.textContent = state.movieId ?? "-";
    elements.showId.textContent = state.showId ?? "-";
    elements.bookingId.textContent = state.bookingId ?? "-";
}

function setScenarioHint(message) {
    elements.scenarioHint.textContent = message;
}

function setFormValues(formId, values) {
    const form = document.getElementById(formId);
    Object.entries(values).forEach(([key, value]) => {
        const field = form.elements.namedItem(key);
        if (field) {
            field.value = value;
        }
    });
}

function updateInputValue(formId, name, value) {
    const form = document.getElementById(formId);
    const field = form?.elements.namedItem(name);
    if (field && value != null) {
        field.value = value;
    }
}

function parseCsvList(value) {
    return String(value)
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);
}

function resolvePoster(title) {
    const normalized = String(title).trim().toLowerCase();
    if (normalized.includes("interstellar")) {
        return posters.interstellar;
    }
    if (normalized.includes("inception")) {
        return posters.inception;
    }
    return posters.default;
}

function tryParseJson(text) {
    if (!text) {
        return null;
    }

    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

function toInt(value) {
    return Number.parseInt(String(value), 10);
}

function formatAmount(value) {
    return new Intl.NumberFormat("en-IN", {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
    }).format(value);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
