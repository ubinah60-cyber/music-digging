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