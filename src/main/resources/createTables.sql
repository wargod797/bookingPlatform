CREATE TABLE city (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE theatre (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city_id INT NOT NULL REFERENCES city(id)
);

CREATE TABLE movie (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL
);

CREATE TABLE show (
    id SERIAL PRIMARY KEY,
    movie_id INT NOT NULL REFERENCES movie(id),
    theatre_id INT NOT NULL REFERENCES theatre(id),
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    price DOUBLE PRECISION NOT NULL
);

CREATE TABLE seat (
    id SERIAL PRIMARY KEY,
    show_id INT NOT NULL REFERENCES show(id),
    seat_number VARCHAR(10) NOT NULL,
    is_booked BOOLEAN DEFAULT FALSE,
    UNIQUE (show_id, seat_number)
);

CREATE TABLE booking (
    id SERIAL PRIMARY KEY,
    show_id INT NOT NULL REFERENCES show(id),
    total_price DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_seat (
    booking_id INT NOT NULL REFERENCES booking(id),
    seat_id INT NOT NULL REFERENCES seat(id),
    PRIMARY KEY (booking_id, seat_id)
);
