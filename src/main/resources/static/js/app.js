document.getElementById("searchBtn")
    .addEventListener("click", searchMusic);

document.getElementById("keyword")
    .addEventListener("keydown", function(event) {

        if (event.key === "Enter") {
            searchMusic();
        }

    });

document.getElementById("modalCloseBtn")
    .addEventListener("click", closeAlbumModal);

async function searchMusic() {

    const keyword =
        document.getElementById("keyword").value.trim();

    const resultArea =
        document.getElementById("resultArea");

    if (keyword === "") {
        resultArea.innerHTML = "<p>검색어를 입력해주세요.</p>";
        return;
    }

    const response =
        await fetch(`/api/music/search?keyword=${keyword}`);

    const musicList =
        await response.json();

    renderMusicList(musicList);

    const artistResponse =
        await fetch(`/api/music/artist?name=${keyword}`);

    const artistList =
        await artistResponse.json();

    renderArtistList(artistList);

    const albumResponse =
        await fetch(`/api/music/albums?artistName=${keyword}`);

    const albumList =
        await albumResponse.json();

    renderAlbumList(albumList);
}

function renderMusicList(musicList) {

    const resultArea =
        document.getElementById("resultArea");

    resultArea.innerHTML = "";

    if (musicList.length === 0) {
        resultArea.innerHTML = "<p>검색 결과가 없습니다.</p>";
        return;
    }

    musicList.forEach(music => {

        resultArea.innerHTML += `
            <div class="music-card">
                <h3>${music.title}</h3>
                <p>가수 : ${music.artist}</p>
                <p>앨범 : ${music.album}</p>
                <p>장르 : ${music.genre}</p>              
            </div>
        `;
    });
}

function renderArtistList(artistList) {

    const artistArea =
        document.getElementById("artistArea");

    artistArea.innerHTML = "";

    if (artistList.length === 0) {
        artistArea.innerHTML =
            "<p>아티스트 정보가 없습니다.</p>";
        return;
    }

    artistList.forEach(artist => {

        artistArea.innerHTML += `
            <div class="music-card">
                <h3>${artist.name}</h3>
                <p>국가 : ${artist.country}</p>
                <p>유형 : ${artist.type}</p>
            </div>
        `;
    });
}

function renderAlbumList(albumList) {

    const albumArea =
        document.getElementById("albumArea");

    albumArea.innerHTML = "";

    if (albumList.length === 0) {
        albumArea.innerHTML =
            "<p>앨범 정보가 없습니다.</p>";
        return;
    }

    albumList.forEach(album => {

        albumArea.innerHTML += `
            <div class="music-card"
             onclick="loadAlbumDetail('${album.id}')">
                <h3>${album.title}</h3>
                <p>유형 : ${album.type}</p>
                <p>발매일 : ${album.firstReleaseDate}</p>
            </div>
        `;
    });
}

async function loadAlbumDetail(albumId) {

    const response =
        await fetch(`/api/music/album-detail?id=${albumId}`);

    const album =
        await response.json();

    const albumDetailArea =
        document.getElementById("albumDetailArea");

    albumDetailArea.innerHTML = `
        <div class="music-card">
            <h3>${album.title}</h3>
            <p>유형 : ${album.type}</p>
            <p>발매일 : ${album.firstReleaseDate}</p>
            <p>ID : ${album.id}</p>
        </div>
    `;

    const trackResponse =
        await fetch(`/api/music/tracks?releaseId=${album.releaseId}`);

    const tracks =
        await trackResponse.json();

    renderTrackList(tracks);

    openAlbumModal();
}

function renderTrackList(tracks) {

    const trackArea =
        document.getElementById("trackArea");

    trackArea.innerHTML = "";

    if (tracks.length === 0) {
        trackArea.innerHTML = "<p>트랙 정보가 없습니다.</p>";
        return;
    }

    tracks.forEach(track => {
        trackArea.innerHTML += `
            <div class="music-card">
                <p>${track.trackNumber}. ${track.title}</p>
                <p>재생시간 : ${track.length}</p>
            </div>
        `;
    });
}

function openAlbumModal() {
    document.getElementById("albumModal")
        .classList.remove("hidden");
}

function closeAlbumModal() {
    document.getElementById("albumModal")
        .classList.add("hidden");
}

