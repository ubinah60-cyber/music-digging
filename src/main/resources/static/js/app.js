document.getElementById("searchBtn")
    .addEventListener("click", searchMusic);

document.getElementById("keyword")
    .addEventListener("keydown", function(event) {

        if (event.key === "Enter") {
            searchMusic();
        }

    });

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