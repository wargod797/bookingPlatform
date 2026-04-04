const state = {
    activeRole: null,
    theatreId: null,
    movieId: null,
    showId: null,
    bookingId: null,
    browseResults: [],
    movies: [],
    partnerCities: [],
    partnerTheatres: [],
    countries: [],
    cities: [],
    adminCities: [],
    selectedShow: null,
    selectedSeats: new Set()
};

const posters = {
    interstellar: "/images/interstellar-poster.svg",
    inception: "/images/inception-poster.svg",
    lovemocktail: "/images/lovemocktail-poster.svg",
    default: "/images/movie-default.svg"
};

const locationCache = {
    version: "v1",
    ttlMs: 1000 * 60 * 60 * 12
};

const elements = {
    roleCards: [...document.querySelectorAll("[data-role]")],
    rolePlaceholder: document.getElementById("role-placeholder"),
    adminPanel: document.getElementById("admin-panel"),
    customerPanel: document.getElementById("customer-panel"),
    stateRole: document.getElementById("state-role"),
    theatreId: document.getElementById("state-theatre-id"),
    movieId: document.getElementById("state-movie-id"),
    showId: document.getElementById("state-show-id"),
    scenarioHint: document.getElementById("scenario-hint"),
    browseResults: document.getElementById("browse-results"),
    userResultNote: document.getElementById("user-result-note"),
    seatSection: document.getElementById("seat-section"),
    seatGrid: document.getElementById("seat-grid"),
    seatSectionTitle: document.getElementById("seat-section-title"),
    seatSectionNote: document.getElementById("seat-section-note"),
    selectedSeatsLabel: document.getElementById("selected-seats-label"),
    selectedShowLabel: document.getElementById("selected-show-label"),
    bookSelectedSeats: document.getElementById("book-selected-seats"),
    bookingSuccess: document.getElementById("booking-success"),
    bookingSuccessTitle: document.getElementById("booking-success-title"),
    bookingSuccessCopy: document.getElementById("booking-success-copy"),
    responseTitle: document.getElementById("response-title"),
    responseStatus: document.getElementById("response-status"),
    responseOutput: document.getElementById("response-output"),
    adminCountrySelect: document.getElementById("admin-country"),
    adminOnboardCitySelect: document.getElementById("admin-onboard-city"),
    adminMovieSelect: document.getElementById("admin-movie"),
    adminCityFilter: document.getElementById("admin-city-filter"),
    adminTheatreSelect: document.getElementById("admin-theatre"),
    movieSelect: document.getElementById("user-movie"),
    countrySelect: document.getElementById("user-country"),
    citySelect: document.getElementById("user-city")
};

bindEvents();
bootstrapUserInputs();
renderRoleSelection();
renderState();
renderEmptyBrowseBoard("Choose the Customer role and search by movie, city, date, and time.");
renderSeatSummary();

function bindEvents() {
    elements.roleCards.forEach((card) => {
        card.addEventListener("click", () => setActiveRole(card.dataset.role));
    });

    document.querySelectorAll("[data-picker-for]").forEach((trigger) => {
        trigger.addEventListener("click", () => openPicker(trigger.dataset.pickerFor));
    });

    document.getElementById("theatre-form").addEventListener("submit", handleTheatreCreate);
    document.getElementById("movie-form").addEventListener("submit", handleMovieCreate);
    document.getElementById("show-form").addEventListener("submit", handleShowCreate);
    document.getElementById("seat-form").addEventListener("submit", handleSeatAllocation);
    document.getElementById("user-discovery-form").addEventListener("submit", handleUserBrowse);

    elements.adminCountrySelect.addEventListener("change", async () => {
        await loadAdminCities(elements.adminCountrySelect.value);
    });

    elements.adminMovieSelect.addEventListener("change", () => {
        state.movieId = toInt(elements.adminMovieSelect.value) || state.movieId;
        renderState();
    });

    elements.adminCityFilter.addEventListener("change", async () => {
        await loadPartnerTheatres(elements.adminCityFilter.value);
    });

    elements.adminTheatreSelect.addEventListener("change", () => {
        state.theatreId = toInt(elements.adminTheatreSelect.value) || null;
        renderState();
    });

    elements.countrySelect.addEventListener("change", async () => {
        await loadCities(elements.countrySelect.value);
    });

    elements.browseResults.addEventListener("click", async (event) => {
        const trigger = event.target.closest("[data-show-id]");
        if (!trigger) {
            return;
        }

        const showId = toInt(trigger.dataset.showId);
        const selectedShow = state.browseResults.find((show) => show.id === showId);
        if (!selectedShow) {
            return;
        }

        state.showId = showId;
        state.selectedShow = selectedShow;
        state.selectedSeats = new Set();
        renderState();
        renderSeatSummary();
        await loadSeatAvailability(showId);
    });

    elements.seatGrid.addEventListener("click", (event) => {
        const seatButton = event.target.closest("[data-seat-number]");
        if (!seatButton || seatButton.classList.contains("is-booked")) {
            return;
        }

        const seatNumber = seatButton.dataset.seatNumber;
        if (state.selectedSeats.has(seatNumber)) {
            state.selectedSeats.delete(seatNumber);
        } else {
            state.selectedSeats.add(seatNumber);
        }

        seatButton.classList.toggle("is-selected", state.selectedSeats.has(seatNumber));
        renderSeatSummary();
    });

    elements.bookSelectedSeats.addEventListener("click", handleBookSelectedSeats);
}

async function bootstrapUserInputs() {
    await Promise.all([loadMovies(), loadCountries(), loadPartnerCities()]);
    if (!state.activeRole) {
        setScenarioHint("Choose Admin to manage catalogue data or Customer to browse and book tickets.");
    }
}

function setActiveRole(role) {
    state.activeRole = role;
    renderRoleSelection();
    renderState();

    if (role === "admin") {
        setScenarioHint("Admin mode active. Add theatres, movies, shows, and seats so customers can book them.");
        return;
    }

    setScenarioHint("Customer mode active. Pick a movie, country, city, date, and time to discover matching shows.");
}

function renderRoleSelection() {
    elements.roleCards.forEach((card) => {
        card.classList.toggle("is-active", card.dataset.role === state.activeRole);
    });

    const hasRole = Boolean(state.activeRole);
    elements.rolePlaceholder.classList.toggle("hidden", hasRole);
    elements.adminPanel.classList.toggle("hidden", state.activeRole !== "admin");
    elements.customerPanel.classList.toggle("hidden", state.activeRole !== "customer");
}

async function handleTheatreCreate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const theatreBrand = String(form.get("theatreBrand") ?? "").trim();
    const theatreBranch = String(form.get("theatreBranch") ?? "").trim();
    const payload = {
        theatreName: buildTheatreName(theatreBrand, theatreBranch),
        cityName: String(form.get("cityName") ?? "").trim()
    };

    if (!payload.theatreName || !payload.cityName) {
        setScenarioHint("Choose a theatre chain and city, then enter a branch or area before creating the theatre.");
        return;
    }

    const result = await requestJson("POST", "/partners/theatres", payload, "Create theatre");
    if (!result.ok) {
        return;
    }

    state.theatreId = result.data.id ?? state.theatreId;
    renderState();
    await loadPartnerCities(result.data.cityName ?? null);
    setScenarioHint(`Theatre ${result.data.theatreName ?? "partner"} created. Next, add a movie and publish a show.`);
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
    renderState();
    await loadMovies(state.movieId);
    setScenarioHint(`Movie ${result.data.title ?? ""} created. Publish a showtime so customers can discover it.`);
}

async function handleShowCreate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const theatreId = toInt(form.get("theatreId"));
    if (!theatreId) {
        setScenarioHint("Choose an admin city and theatre name from the dropdowns before creating a show.");
        return;
    }

    const payload = {
        movieId: toInt(form.get("movieId")),
        theatreId,
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
    renderState();
    setScenarioHint(`Show ${state.showId} is live. Allocate seats before attempting the customer booking flow.`);
}

async function handleSeatAllocation(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const showId = toInt(form.get("showId"));
    const payload = {
        seatNumbers: parseCsv(form.get("seatNumbers"))
    };

    const result = await requestJson("POST", `/partners/shows/${showId}/seats`, payload, "Allocate seats");
    if (!result.ok) {
        return;
    }

    setScenarioHint(`Seat inventory added for show ${showId}. Switch to the Customer role to browse and book.`);
}

async function handleUserBrowse(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const movieId = toInt(form.get("movieId"));
    const city = form.get("city");
    const date = form.get("date");
    const preferredTime = form.get("time");

    if (!movieId || !city || !date) {
        setScenarioHint("Please choose a movie, city, and date before browsing.");
        return;
    }

    renderBrowseSkeletons();
    const query = new URLSearchParams({
        movieId: String(movieId),
        city,
        date
    });

    const result = await requestJson("GET", `/browse/shows?${query.toString()}`, null, "Browse shows");
    if (!result.ok) {
        renderEmptyBrowseBoard("Browse request failed. Check the response console for the backend message.");
        return;
    }

    const shows = Array.isArray(result.data) ? result.data : [];
    const filtered = filterByPreferredTime(shows, preferredTime);

    state.browseResults = filtered;
    state.movieId = movieId;
    renderState();

    if (!filtered.length) {
        const message = preferredTime
                ? `No shows matched ${city} on ${date} around ${preferredTime}. Try another time or remove the time filter.`
                : `No shows matched ${city} on ${date}.`;
        renderEmptyBrowseBoard(message);
        elements.userResultNote.textContent = "No matching shows found.";
        elements.seatSection.classList.add("hidden");
        return;
    }

    elements.userResultNote.textContent = preferredTime
            ? `Showing exact matches for ${preferredTime}.`
            : "Showing all available showtimes for the selected date.";
    renderBrowseResults(filtered);
    setScenarioHint(`Found ${filtered.length} show(s). Pick one to load seat availability.`);
}

async function loadMovies(selectedMovieId = null) {
    const result = await requestJson("GET", "/movies", null, "Load movies", { silent: true });
    if (!result.ok) {
        state.movies = [];
        renderMovieOptions(selectedMovieId);
        return;
    }

    state.movies = Array.isArray(result.data) ? result.data : [];
    renderMovieOptions(selectedMovieId);
}

async function loadCountries() {
    const cachedCountries = readCache(cacheKey("countries"));
    if (Array.isArray(cachedCountries) && cachedCountries.length) {
        state.countries = cachedCountries;
        renderCountryOptions();
        const initialCountry = state.countries.includes("India") ? "India" : state.countries[0];
        if (initialCountry) {
            elements.countrySelect.value = initialCountry;
            elements.adminCountrySelect.value = initialCountry;
            await Promise.all([loadCities(initialCountry), loadAdminCities(initialCountry)]);
        }
        return;
    }

    const result = await requestJson("GET", "/ui/locations/countries", null, "Load countries", { silent: true });
    if (!result.ok) {
        state.countries = [];
        renderCountryOptions();
        return;
    }

    state.countries = Array.isArray(result.data) ? result.data : [];
    writeCache(cacheKey("countries"), state.countries);
    renderCountryOptions();
    const initialCountry = state.countries.includes("India") ? "India" : state.countries[0];
    if (initialCountry) {
        elements.countrySelect.value = initialCountry;
        elements.adminCountrySelect.value = initialCountry;
        await Promise.all([loadCities(initialCountry), loadAdminCities(initialCountry)]);
    }
}

async function loadPartnerCities(preferredCity = null) {
    const result = await requestJson("GET", "/partners/theatres/cities", null, "Load partner cities", { silent: true });
    if (!result.ok) {
        state.partnerCities = [];
        renderPartnerCityOptions(preferredCity);
        renderPartnerTheatreOptions();
        return;
    }

    state.partnerCities = Array.isArray(result.data) ? result.data : [];
    renderPartnerCityOptions(preferredCity);
    const cityToLoad = preferredCity && state.partnerCities.includes(preferredCity)
            ? preferredCity
            : elements.adminCityFilter.value;
    await loadPartnerTheatres(cityToLoad);
}

async function loadPartnerTheatres(city) {
    if (!city) {
        state.partnerTheatres = [];
        renderPartnerTheatreOptions();
        return;
    }

    const query = new URLSearchParams({ city });
    const result = await requestJson("GET", `/partners/theatres?${query.toString()}`, null, "Load partner theatres", { silent: true });
    if (!result.ok) {
        state.partnerTheatres = [];
        renderPartnerTheatreOptions();
        return;
    }

    state.partnerTheatres = Array.isArray(result.data) ? result.data : [];
    renderPartnerTheatreOptions();
}

async function loadCities(country) {
    elements.citySelect.innerHTML = `<option>Loading cities...</option>`;
    const cachedCities = readCache(cacheKey("cities", country));
    if (Array.isArray(cachedCities) && cachedCities.length) {
        state.cities = cachedCities;
        renderCityOptions();
        return;
    }

    const query = new URLSearchParams({ country });
    const result = await requestJson("GET", `/ui/locations/cities?${query.toString()}`, null, "Load cities", { silent: true });
    if (!result.ok) {
        state.cities = [];
        renderCityOptions();
        return;
    }

    state.cities = Array.isArray(result.data) ? result.data : [];
    writeCache(cacheKey("cities", country), state.cities);
    renderCityOptions();
}

async function loadAdminCities(country) {
    elements.adminOnboardCitySelect.innerHTML = `<option>Loading cities...</option>`;
    const cachedCities = readCache(cacheKey("cities", country));
    if (Array.isArray(cachedCities) && cachedCities.length) {
        state.adminCities = cachedCities;
        renderAdminCityOptions();
        return;
    }

    const query = new URLSearchParams({ country });
    const result = await requestJson("GET", `/ui/locations/cities?${query.toString()}`, null, "Load admin cities", { silent: true });
    if (!result.ok) {
        state.adminCities = [];
        renderAdminCityOptions();
        return;
    }

    state.adminCities = Array.isArray(result.data) ? result.data : [];
    writeCache(cacheKey("cities", country), state.adminCities);
    renderAdminCityOptions();
}

async function loadSeatAvailability(showId) {
    elements.seatSection.classList.remove("hidden");
    elements.seatGrid.classList.add("is-loading");
    elements.seatGrid.innerHTML = Array.from({ length: 12 }, () => `<div class="seat-skeleton"></div>`).join("");

    const result = await requestJson("GET", `/shows/${showId}/seats`, null, "Load seat availability");
    if (!result.ok) {
        elements.seatSectionNote.textContent = "Unable to load seats for this show.";
        elements.seatGrid.classList.remove("is-loading");
        elements.seatGrid.innerHTML = "";
        return;
    }

    const seats = Array.isArray(result.data) ? result.data : [];
    elements.seatGrid.classList.remove("is-loading");
    renderSeatGrid(seats);
}

async function handleBookSelectedSeats() {
    if (!state.selectedShow || !state.selectedSeats.size) {
        setScenarioHint("Select at least one seat before booking.");
        return;
    }

    const payload = {
        showId: state.selectedShow.id,
        seats: [...state.selectedSeats]
    };

    const result = await requestJson("POST", "/bookings", payload, "Book selected seats");
    if (!result.ok) {
        return;
    }

    state.bookingId = result.data.id ?? state.bookingId;
    state.selectedSeats = new Set();
    renderState();
    renderSeatSummary();
    renderBookingSuccess(result.data);
    await loadSeatAvailability(state.selectedShow.id);
    setScenarioHint(`Booking ${state.bookingId} confirmed. Final total returned by backend: ${formatAmount(result.data.totalPrice ?? 0)}.`);
}

function renderMovieOptions(selectedMovieId = null) {
    if (!state.movies.length) {
        elements.movieSelect.innerHTML = `<option value="">No movies available</option>`;
        elements.adminMovieSelect.innerHTML = `<option value="">No movies available</option>`;
        return;
    }

    const optionsMarkup = state.movies.map((movie) => `
        <option value="${movie.id}" ${String(selectedMovieId ?? state.movieId ?? "") === String(movie.id) ? "selected" : ""}>
            ${escapeHtml(movie.title)} - ${escapeHtml(movie.language)}
        </option>
    `).join("");

    elements.movieSelect.innerHTML = optionsMarkup;
    elements.adminMovieSelect.innerHTML = optionsMarkup;

    const current = toInt(elements.movieSelect.value);
    if (current) {
        state.movieId = current;
        renderState();
    }
}

function renderPartnerCityOptions(preferredCity = null) {
    if (!state.partnerCities.length) {
        elements.adminCityFilter.innerHTML = `<option value="">Create a theatre first</option>`;
        return;
    }

    elements.adminCityFilter.innerHTML = state.partnerCities.map((city) => `
        <option value="${escapeHtml(city)}">${escapeHtml(city)}</option>
    `).join("");

    if (preferredCity && state.partnerCities.includes(preferredCity)) {
        elements.adminCityFilter.value = preferredCity;
    }
}

function renderPartnerTheatreOptions() {
    if (!state.partnerTheatres.length) {
        elements.adminTheatreSelect.innerHTML = `<option value="">No theatres available</option>`;
        state.theatreId = null;
        renderState();
        return;
    }

    elements.adminTheatreSelect.innerHTML = state.partnerTheatres.map((theatre) => `
        <option value="${theatre.id}">${escapeHtml(theatre.theatreName)} - ${escapeHtml(theatre.cityName)}</option>
    `).join("");

    const selectedTheatreId = toInt(elements.adminTheatreSelect.value);
    if (selectedTheatreId) {
        state.theatreId = selectedTheatreId;
        renderState();
    }
}

function renderCountryOptions() {
    if (!state.countries.length) {
        elements.countrySelect.innerHTML = `<option value="">No countries available</option>`;
        elements.adminCountrySelect.innerHTML = `<option value="">No countries available</option>`;
        return;
    }

    const optionsMarkup = state.countries.map((country) => `
        <option value="${escapeHtml(country)}">${escapeHtml(country)}</option>
    `).join("");

    elements.countrySelect.innerHTML = optionsMarkup;
    elements.adminCountrySelect.innerHTML = optionsMarkup;
}

function renderCityOptions() {
    if (!state.cities.length) {
        elements.citySelect.innerHTML = `<option value="">No cities available</option>`;
        return;
    }

    elements.citySelect.innerHTML = state.cities.map((city) => `
        <option value="${escapeHtml(city)}">${escapeHtml(city)}</option>
    `).join("");

    if (state.cities.includes("Mumbai")) {
        elements.citySelect.value = "Mumbai";
    }
}

function renderAdminCityOptions() {
    if (!state.adminCities.length) {
        elements.adminOnboardCitySelect.innerHTML = `<option value="">No cities available</option>`;
        return;
    }

    elements.adminOnboardCitySelect.innerHTML = state.adminCities.map((city) => `
        <option value="${escapeHtml(city)}">${escapeHtml(city)}</option>
    `).join("");

    if (state.adminCities.includes("Mumbai")) {
        elements.adminOnboardCitySelect.value = "Mumbai";
    }
}

function renderBrowseResults(results) {
    elements.browseResults.innerHTML = results.map((show) => {
        const title = show.movie?.title ?? "Movie";
        const theatre = show.theatre?.name ?? "Theatre";
        const city = show.theatre?.city?.name ?? "City";
        const poster = resolvePoster(title);
        const price = typeof show.price === "number" ? formatAmount(show.price) : show.price;
        const showTime = normalizeTime(show.showTime);

        return `
            <article class="show-card">
                <img class="show-poster" src="${poster}" alt="${escapeHtml(title)} poster">
                <div class="show-copy">
                    <div>
                        <h3>${escapeHtml(title)}</h3>
                        <p>${escapeHtml(theatre)} - ${escapeHtml(city)}</p>
                    </div>
                    <div class="show-meta">
                        <span>Show #${escapeHtml(String(show.id ?? "-"))}</span>
                        <span>${escapeHtml(String(show.showDate ?? "-"))}</span>
                        <span>${escapeHtml(showTime)}</span>
                        <span>${escapeHtml(String(price))}</span>
                    </div>
                    <button type="button" data-show-id="${escapeHtml(String(show.id ?? ""))}">Choose seats</button>
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

function renderEmptyBrowseBoard(message) {
    elements.browseResults.innerHTML = `
        <div class="empty-panel">
            <h3>No shows yet</h3>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

function renderSeatGrid(seats) {
    if (!seats.length) {
        elements.seatGrid.innerHTML = `<div class="empty-panel"><p>No seat inventory found for this show yet.</p></div>`;
        elements.seatSectionNote.textContent = "Ask an admin to allocate seats before booking.";
        return;
    }

    elements.seatSectionTitle.textContent = `Choose seats for Show #${state.selectedShow?.id ?? "-"}`;
    elements.seatSectionNote.textContent = "Unavailable seats are marked and cannot be selected.";
    elements.seatGrid.innerHTML = seats.map((seat) => `
        <button
            type="button"
            class="seat${seat.booked ? " is-booked" : ""}${state.selectedSeats.has(seat.seatNumber) ? " is-selected" : ""}"
            data-seat-number="${escapeHtml(seat.seatNumber)}"
            ${seat.booked ? "disabled" : ""}>
            ${escapeHtml(seat.seatNumber)}
        </button>
    `).join("");
}

function renderSeatSummary() {
    if (!state.selectedShow) {
        elements.selectedSeatsLabel.textContent = "None selected";
        elements.selectedShowLabel.textContent = "Choose a show to load its seat map.";
        elements.bookSelectedSeats.disabled = true;
        return;
    }

    const selectedSeats = [...state.selectedSeats].sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
    elements.selectedSeatsLabel.textContent = selectedSeats.length ? selectedSeats.join(", ") : "None selected";
    elements.selectedShowLabel.textContent = `${state.selectedShow.movie?.title ?? "Movie"} at ${state.selectedShow.theatre?.name ?? "Theatre"} - ${normalizeTime(state.selectedShow.showTime)}`;
    elements.bookSelectedSeats.disabled = selectedSeats.length === 0;
}

function renderBookingSuccess(booking) {
    elements.bookingSuccess.classList.remove("hidden");
    elements.bookingSuccessTitle.textContent = `Booking #${booking.id ?? "-"} confirmed`;
    elements.bookingSuccessCopy.textContent = `Seats ${formatSeatList(booking.seats)} booked successfully. Backend returned a final total of ${formatAmount(booking.totalPrice ?? 0)}.`;
}

function renderState() {
    elements.stateRole.textContent = state.activeRole ? capitalize(state.activeRole) : "Choose one";
    elements.theatreId.textContent = state.theatreId ?? "-";
    elements.movieId.textContent = state.movieId ?? "-";
    elements.showId.textContent = state.showId ?? "-";
}

function setScenarioHint(message) {
    elements.scenarioHint.textContent = message;
}

async function requestJson(method, path, payload, label, options = {}) {
    if (!options.silent) {
        renderResponseSkeleton(label);
    }

    try {
        const requestOptions = {
            method,
            headers: {}
        };

        if (payload != null) {
            requestOptions.headers["Content-Type"] = "application/json";
            requestOptions.body = JSON.stringify(payload);
        }

        const response = await fetch(path, requestOptions);
        const rawText = await response.text();
        const parsed = tryParseJson(rawText);
        const pretty = parsed == null ? rawText : JSON.stringify(parsed, null, 2);

        if (!response.ok) {
            if (!options.silent) {
                renderResponse(`${label} failed`, `${response.status} ${parsed?.message || parsed?.error || response.statusText}`, "error", pretty);
            }
            return { ok: false, response, data: parsed };
        }

        if (!options.silent) {
            renderResponse(label, `${response.status} ${response.statusText}`, "success", pretty || "{}");
        }
        return { ok: true, response, data: parsed };
    } catch (error) {
        if (!options.silent) {
            renderResponse(
                `${label} failed`,
                "Network error",
                "error",
                JSON.stringify({
                    message: error.message,
                    hint: "Make sure the Spring Boot app is running and has internet access if you are loading cities online."
                }, null, 2)
            );
        }
        return { ok: false, error };
    }
}

function renderResponseSkeleton(label) {
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
        </div>
    `;
}

function renderResponse(title, status, tone, body) {
    elements.responseTitle.textContent = title;
    elements.responseStatus.textContent = status;
    elements.responseStatus.className = `response-pill ${tone}`;
    elements.responseOutput.innerHTML = `<pre>${escapeHtml(body)}</pre>`;
}

function filterByPreferredTime(shows, preferredTime) {
    if (!preferredTime) {
        return shows;
    }

    return shows.filter((show) => normalizeTime(show.showTime) === preferredTime);
}

function normalizeTime(value) {
    if (!value) {
        return "--:--";
    }

    return String(value).slice(0, 5);
}

function resolvePoster(title) {
    const normalized = String(title).trim().toLowerCase();
    if (normalized.includes("interstellar")) {
        return posters.interstellar;
    }
    if (normalized.includes("inception")) {
        return posters.inception;
    }
    if (normalized.includes("lovemocktail") || normalized.includes("love mocktail")) {
        return posters.lovemocktail;
    }
    return posters.default;
}

function formatSeatList(seats) {
    if (!Array.isArray(seats) || !seats.length) {
        return "none";
    }

    return seats
        .map((seat) => seat.seatNumber)
        .filter(Boolean)
        .join(", ");
}

function parseCsv(value) {
    return String(value)
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);
}

function buildTheatreName(brand, branch) {
    return [brand, branch]
        .map((value) => String(value ?? "").trim())
        .filter(Boolean)
        .join(" ");
}

function openPicker(fieldId) {
    const field = document.getElementById(fieldId);
    if (!field) {
        return;
    }

    field.focus();
    if (typeof field.showPicker === "function") {
        field.showPicker();
        return;
    }

    field.click();
}

function updateInputValue(formId, fieldName, value) {
    const field = document.getElementById(formId)?.elements.namedItem(fieldName);
    if (field && value != null) {
        field.value = value;
    }
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

function capitalize(value) {
    return value.charAt(0).toUpperCase() + value.slice(1);
}

function cacheKey(scope, qualifier = "all") {
    return `booking-platform:${locationCache.version}:${scope}:${normalizeCacheKey(qualifier)}`;
}

function normalizeCacheKey(value) {
    return String(value ?? "all")
        .trim()
        .toLowerCase()
        .replace(/\s+/g, "-");
}

function readCache(key) {
    try {
        const raw = window.localStorage.getItem(key);
        if (!raw) {
            return null;
        }

        const parsed = JSON.parse(raw);
        if (!parsed || parsed.expiresAt < Date.now()) {
            window.localStorage.removeItem(key);
            return null;
        }

        return parsed.data ?? null;
    } catch {
        return null;
    }
}

function writeCache(key, data) {
    try {
        const payload = {
            expiresAt: Date.now() + locationCache.ttlMs,
            data
        };
        window.localStorage.setItem(key, JSON.stringify(payload));
    } catch {
        // Ignore storage quota or private-mode failures and continue without client caching.
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
